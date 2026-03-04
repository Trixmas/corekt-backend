package lab.corekt.backend.modules.project.file.taiga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.project.file.taiga.dto.ProjectResponse;
import lab.corekt.backend.modules.project.file.taiga.dto.TaigaLoginResponse;
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
    public ResponseEntity<TaigaLoginResponse> loginToTaiga(
            @Valid @RequestBody LoginRequest request
    ) {
        String responseBody;

        try {
            responseBody = HTTPService.makeRequest(TAIGA_URL + "/auth", "POST", String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", request.getPassword(), request.getUsername()));
        } catch (IOException | InterruptedException _) {
            return ResponseEntity.status(500).body(new TaigaLoginResponse(false, "Failed to connect to Taiga"));
        }

        if (responseBody == null || responseBody.isBlank()) {
            return ResponseEntity.badRequest().body(new TaigaLoginResponse(false, "Invalid response from Taiga"));
        }
        try {
            String token = JsonUtils.readJson(responseBody, "auth_token");
            String userId = JsonUtils.readJson(responseBody, "id");
            if (token == null || userId == null) {
                return ResponseEntity.badRequest().body(new TaigaLoginResponse(false, "Invalid credentials"));
            }

            // Store the token server-side
            taigaTokenService.storeTaigaToken(token, userId);

            return ResponseEntity.ok(new TaigaLoginResponse(true, "Successfully logged in to Taiga"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new TaigaLoginResponse(false, "An error occurred: " + e.getMessage()));
        }

    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectResponse> getProjects() {
        String taigaToken = taigaTokenService.getTaigaToken();
        String taigaUserId = taigaTokenService.getTaigaUserId();

        if (taigaToken == null || taigaUserId == null) {
            return ResponseEntity.badRequest().body(new ProjectResponse());
        }

        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/projects?member=" + taigaUserId, "GET", "", taigaToken);
            Map<String, String> projects = new HashMap<>();

            ArrayNode nn = JsonUtils.getJsonNodes(body);
            for (JsonNode n : nn) {
                projects.put(n.get("name").asText(), n.get("id").asText());
            }

            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(new ProjectResponse());
        }
    }


    @GetMapping("/us")
    public ResponseEntity<String> getUs(@Valid @RequestParam String projectId) {
        String taigaToken = taigaTokenService.getTaigaToken();

        if (taigaToken == null) {
            return ResponseEntity.badRequest().body("Not authenticated with Taiga");
        }

        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/userstories?project=" + projectId, "GET", "", taigaToken);

            Map<String, String> userStories = new HashMap<>();

            ArrayNode nn = JsonUtils.getJsonNodes(body);
            for (JsonNode n : nn) {
                String storyId = n.get("id").asText();
                String subject = n.get("subject").asText();
                String description = getUserStoryDetails(storyId, taigaToken).asText();
                userStories.put(storyId, subject + ": " + description);
            }
//            TODO: store US in DB


            taigaTokenService.removeTaigaToken();
            return ResponseEntity.ok(userStories.toString());
        } catch (IOException | InterruptedException _) {
            return ResponseEntity.status(500).body("Error fetching user stories");
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
