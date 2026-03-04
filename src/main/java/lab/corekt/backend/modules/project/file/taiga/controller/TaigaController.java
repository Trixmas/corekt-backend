package lab.corekt.backend.modules.project.file.taiga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginResponse;
import lab.corekt.backend.modules.project.file.taiga.dto.ProjectResponse;
import lab.corekt.backend.modules.project.file.taiga.dto.UsResponse;
import lab.corekt.backend.modules.project.file.taiga.service.HTTPService;
import lab.corekt.backend.modules.project.file.taiga.service.TaigaTokenService;
import lab.corekt.backend.utils.json.JsonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/project/taiga")
class TaigaController {

    private static final String TAIGA_URL = "https://api.taiga.io/api/v1";

    private final TaigaTokenService taigaTokenService;

    public TaigaController(TaigaTokenService taigaTokenService) {
        this.taigaTokenService = taigaTokenService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(
            @Valid @RequestBody LoginRequest request
    ) {
        String responseBody;

        try {
            responseBody = HTTPService.makeRequest(TAIGA_URL + "/auth", "POST", String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", request.getPassword(), request.getUsername()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new LoginResponse(false, "Failed to connect to Taiga: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(new LoginResponse(false, "Request to Taiga was interrupted"));
        }

        if (responseBody == null || responseBody.isBlank()) {
            return ResponseEntity.badRequest().body(new LoginResponse(false, "Invalid response from Taiga"));
        }
        try {
            String token = JsonUtils.readJson(responseBody, "auth_token");
            String userId = JsonUtils.readJson(responseBody, "id");
            if (token == null || userId == null) {
                return ResponseEntity.badRequest().body(new LoginResponse(false, "Invalid credentials or missing token"));
            }

            // Store the token server-side
            taigaTokenService.storeTaigaToken(token, userId);

            return ResponseEntity.ok(new LoginResponse(true, "Successfully logged in to Taiga"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(new LoginResponse(false, "An error occurred: " + e.getMessage()));
        }

    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectResponse> getProjects() {
        String taigaToken = taigaTokenService.getTaigaToken();
        String taigaUserId = taigaTokenService.getTaigaUserId();

        if (taigaToken == null || taigaUserId == null) {
            return ResponseEntity.status(401).body(new ProjectResponse(false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/projects?member=" + taigaUserId, "GET", "", taigaToken);
            Map<String, String> projects = new HashMap<>();

            ArrayNode nn = JsonUtils.getJsonNodes(body);
            if (nn != null) {
                for (JsonNode n : nn) {
                    try {
                        projects.put(n.get("name").asText(), n.get("id").asText());
                    } catch (Exception e) {
                        System.err.println("Error processing project: " + e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ProjectResponse(false, "Failed to fetch projects from Taiga: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(new ProjectResponse(false, "Request to Taiga was interrupted"));
        }
    }


    @GetMapping("/us")
    public ResponseEntity<UsResponse> getUs(@Valid @RequestParam String projectId) {
        String taigaToken = taigaTokenService.getTaigaToken();

        if (taigaToken == null) {
            return ResponseEntity.status(401).body(new UsResponse(null, false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/userstories?project=" + projectId, "GET", "", taigaToken);

            ArrayNode nn = JsonUtils.getJsonNodes(body);
            if (nn == null || nn.isEmpty()) {
                return ResponseEntity.ok(new UsResponse(new java.util.ArrayList<>(), true, null));
            }

            java.util.List<String> userStories = new java.util.ArrayList<>();

            for (JsonNode n : nn) {
                try {
                    String storyId = n.get("id").asText();
                    String subject = n.get("subject").asText();
                    String description = getUserStoryDetails(storyId, taigaToken).asText();
                    userStories.add(subject + ": " + description);
                } catch (Exception e) {
                    System.err.println("Error processing user story: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(new UsResponse(userStories));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new UsResponse(null, false, "Failed to fetch user stories from Taiga: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body(new UsResponse(null, false, "Request to Taiga was interrupted"));
        } finally {
            taigaTokenService.removeTaigaToken();
        }
    }

    private JsonNode getUserStoryDetails(String storyId, String taigaToken) {

        try {
            String res = HTTPService.makeRequest(TAIGA_URL + "/userstories/" + storyId, "GET", "", taigaToken);


            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(res).get("description");
        } catch (IOException | InterruptedException _) {
            return new ObjectMapper().createObjectNode();
        }
    }


}
