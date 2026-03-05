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

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/project/taiga")
class TaigaController {

    private final TaigaTokenService taigaTokenService;
    private final HTTPService httpService;

    public TaigaController(TaigaTokenService taigaTokenService, HTTPService httpService) {
        this.taigaTokenService = taigaTokenService;
        this.httpService = httpService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            String responseBody = httpService.post(
                    "/auth",
                    String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", request.getPassword(), request.getUsername())
            );

            if (responseBody == null || responseBody.isBlank()) {
                return ResponseEntity.badRequest().body(new LoginResponse(false, "Invalid response from Taiga"));
            }

            String token = JsonUtils.readJson(responseBody, "auth_token");
            String userId = JsonUtils.readJson(responseBody, "id");
            if (token == null || userId == null) {
                return ResponseEntity.badRequest().body(new LoginResponse(false, "Invalid credentials or missing token"));
            }

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
            String body = httpService.get("/projects?member=" + taigaUserId, taigaToken, null);
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
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ProjectResponse(false, "Failed to fetch projects from Taiga: " + e.getMessage()));
        }
    }


    @GetMapping("/us")
    public ResponseEntity<UsResponse> getUs(@Valid @RequestParam String projectId) {
        String taigaToken = taigaTokenService.getTaigaToken();

        if (taigaToken == null) {
            return ResponseEntity.status(401).body(new UsResponse(null, false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            java.util.List<String> userStories = new java.util.ArrayList<>();

            System.out.printf("Fetching user stories from Taiga (project: %s)%n", projectId);

            Map<String, String> headers = new HashMap<>();
            headers.put("x-disable-pagination", "True");

            String body = httpService.get("/userstories?project=" + projectId, taigaToken, headers);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseNode = mapper.readTree(body);

            ArrayNode results;
            if (responseNode.isArray()) {
                results = (ArrayNode) responseNode;
            } else {
                results = (ArrayNode) responseNode.get("results");
            }

            if (results != null && !results.isEmpty()) {
                for (JsonNode n : results) {
                    try {
                        String storyId = n.get("id").asText();
                        String subject = n.get("subject").asText();
                        String description = getUserStoryDetails(storyId, taigaToken).asText();
                        //TODO: enregistrer en bd
//                        TODO: faire un objet json de l'US
                        userStories.add(subject + ": " + description);
                    } catch (Exception e) {
                        System.err.println("Error processing user story: " + e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(new UsResponse(userStories));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new UsResponse(null, false, "Failed to fetch user stories from Taiga: " + e.getMessage()));
        } finally {
            taigaTokenService.removeTaigaToken();
        }
    }

    private JsonNode getUserStoryDetails(String storyId, String taigaToken) {
        try {
            String res = httpService.get("/userstories/" + storyId, taigaToken, null);
            return new ObjectMapper().readTree(res).get("description");
        } catch (Exception e) {
            return new ObjectMapper().createObjectNode();
        }
    }


}
