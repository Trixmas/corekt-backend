package lab.corekt.backend.modules.project.file.taiga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginResponse;
import lab.corekt.backend.modules.project.file.taiga.dto.ProjectResponse;
import lab.corekt.backend.modules.project.file.taiga.service.HTTPService;
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


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(
            @Valid @RequestBody LoginRequest request
    ) {
        String responseBody;

        try {
            responseBody = HTTPService.makeRequest(TAIGA_URL + "/auth", "POST", String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", request.getPassword(), request.getUsername()));
        } catch (IOException | InterruptedException _) {
            return ResponseEntity.status(500).body(new LoginResponse());
        }

        if (responseBody == null || responseBody.isBlank()) {
            return ResponseEntity.badRequest().body(new LoginResponse());
        }
        try {
            String token = JsonUtils.readJson(responseBody, "auth_token");
            String userId = JsonUtils.readJson(responseBody, "id");
            if (token == null || userId == null) {
                return ResponseEntity.badRequest().body(new LoginResponse());
            }
            return ResponseEntity.ok(new LoginResponse(token, userId));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new LoginResponse());
        }

    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectResponse> getProjects(@Valid @RequestParam String taigaUserId, @Valid @RequestParam String taigaToken) {


        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/projects?member=" + taigaUserId, "GET", "");

            Map<String, String> projects = new HashMap<>();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonObject = mapper.readTree(body);
            ArrayNode nn = (ArrayNode) jsonObject;
            for (JsonNode n : nn) {
                projects.put(n.get("name").asText(), n.get("id").asText());
            }

            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (IOException | InterruptedException _) {
            return ResponseEntity.status(500).body(new ProjectResponse());
        }
    }

    @GetMapping("/us")
    public ResponseEntity<String> getUs(@Valid @RequestParam String projectId, @Valid @RequestParam String taigaToken) {


        try {
            String body = HTTPService.makeRequest(TAIGA_URL + "/userstories?project=" + projectId, "GET", "");

            Map<String, String> userStories = new HashMap<>();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonObject = mapper.readTree(body);
            ArrayNode nn = (ArrayNode) jsonObject;
            for (JsonNode n : nn) {
                String storyId = n.get("id").asText();
                String subject = n.get("subject").asText();
                String description = getUserStoryDetails(storyId, taigaToken).asText();
                userStories.put(storyId, subject + ": " + description);
            }
            return ResponseEntity.ok(userStories.toString());
        } catch (IOException | InterruptedException _) {
            return ResponseEntity.status(500).body("Error fetching user stories");
        }
    }

    public JsonNode getUserStoryDetails(String storyId, String taigaToken) {

        try {
            String res = HTTPService.makeRequest(TAIGA_URL + "/userstories/" + storyId, "GET", "");


            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(res).get("description");
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }


}
