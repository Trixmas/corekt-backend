package lab.corekt.backend.modules.project.file.taiga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.project.file.taiga.dto.LoginResponse;
import lab.corekt.backend.modules.project.file.taiga.dto.ProjectResponse;
import lab.corekt.backend.utils.json.JsonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/project/taiga")
class TaigaController {

    private final String taigaUrl = "https://api.taiga.io/api/v1";
    private final HttpClient client = HttpClient.newHttpClient();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
//        Craft the request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(taigaUrl + "/auth"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", request.getPassword(), request.getUsername())))
                    .build();
//        Make the request to taiga API
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            try {
                String token = JsonUtils.readJson(body, "auth_token");
                String userId = JsonUtils.readJson(body, "id");
                if (token == null || userId == null) {
                    return ResponseEntity.badRequest().body(new LoginResponse());
                }
                return ResponseEntity.ok(new LoginResponse(token, userId));

            } catch (Exception e) {
                return ResponseEntity.badRequest().body(new LoginResponse());
            }

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(new LoginResponse());
        }

    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectResponse> getProjects(@Valid @RequestParam String taigaUserId, @Valid @RequestParam String taigaToken) {


        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(taigaUrl + "/projects?member=" + taigaUserId))
                    .header("Authorization", "Bearer " + taigaToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println(body);

            Map<String, String> projects = new HashMap<>();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonObject = mapper.readTree(body);
            ArrayNode nn = (ArrayNode) jsonObject;
            for (JsonNode n : nn) {
                projects.put(n.get("name").asText(), n.get("id").asText());
            }

            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body(new ProjectResponse());
        }
    }

    @GetMapping("/us")
    public ResponseEntity<String> getUs(@Valid @RequestParam String projectId, @Valid @RequestParam String taigaToken) {


        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(taigaUrl + "/userstories?project=" + projectId))
                    .header("Authorization", "Bearer " + taigaToken)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

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
            System.out.println(userStories);
            return ResponseEntity.ok(userStories.toString());
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Error fetching user stories");
        }
    }

    public JsonNode getUserStoryDetails(String storyId, String taigaToken) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(taigaUrl + "/userstories/" + storyId))
                .header("Authorization", "Bearer " + taigaToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String res = response.body();


            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(res).get("description");
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }


}
