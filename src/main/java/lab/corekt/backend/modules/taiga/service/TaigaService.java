package lab.corekt.backend.modules.taiga.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import lab.corekt.backend.modules.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.taiga.dto.UserStory;
import lab.corekt.backend.modules.taiga.exception.TaigaAuthException;
import lab.corekt.backend.modules.taiga.exception.TaigaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

@Service
public class TaigaService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final FileService fileService;
    private final ParsedDataRepository parsedDataRepository;

    public TaigaService(
        RestClient.Builder restClientBuilder,
        @Value("${taiga.api.base-url}") String taigaBaseUrl,
        FileService fileService,
        ParsedDataRepository parsedDataRepository
    ) {
        this.restClient = restClientBuilder
            .baseUrl(taigaBaseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.fileService = fileService;
        this.parsedDataRepository = parsedDataRepository;
    }

    public TaigaCredentials authenticate(String username, String password) {
        return executeTaigaCall(
            () -> toCredentials(post("/auth", Map.of(
                "username", username,
                "password", password,
                "type", "normal"
            ))),
            "Invalid Taiga credentials",
            "Failed to reach Taiga API"
        );
    }

    public Map<String, String> fetchProjects(String token, String userId) {
        return executeTaigaCall(
            () -> toProjects(get("/projects?member={id}", token, userId)),
            "Taiga token is invalid or expired",
            "Failed to fetch projects from Taiga"
        );
    }

    @Transactional
    public List<UserStory> fetchAndSaveUserStories(String taigaProjectId, String token, VersionEntity version) {
        return executeTaigaCall(() -> {
            List<UserStory> userStories = fetchUserStories(taigaProjectId, token);
            if (userStories.isEmpty()) {
                return List.of();
            }
            persistUserStories(userStories, version);
            return userStories;
        }, "Taiga token is invalid or expired", "Failed to fetch user stories from Taiga");
    }

    private TaigaCredentials toCredentials(JsonNode root) {
        JsonNode token = root.get("auth_token");
        JsonNode userId = root.get("id");

        if (token == null || userId == null) {
            throw new TaigaAuthException("Invalid credentials");
        }

        return new TaigaCredentials(token.asText(), userId.asText());
    }

    private Map<String, String> toProjects(JsonNode root) {
        Map<String, String> projects = new LinkedHashMap<>();
        if (!root.isArray()) {
            return projects;
        }

        for (JsonNode node : root) {
            projects.put(node.get("name").asText(), node.get("id").asText());
        }
        return projects;
    }

    private List<UserStory> fetchUserStories(String taigaProjectId, String token) {
        JsonNode root = get(
            "/userstories?project={id}",
            token,
            Map.of("x-disable-pagination", "True"),
            taigaProjectId
        );

        ArrayNode results = toArray(root);
        if (results == null || results.isEmpty()) return List.of();

        return StreamSupport.stream(results.spliterator(), false)
            .map(node -> new UserStory(
                node.get("subject").asText(),
                fetchStoryDescription(node.get("id").asText(), token)
            ))
            .toList();
    }

    private void persistUserStories(List<UserStory> userStories, VersionEntity version) {
        var file = fileService.getOrCreate(FileType.US, "User Stories", version);

        List<ParsedDataEntity> batch = userStories.stream()
            .map(us -> {
                ParsedDataEntity parsedData = new ParsedDataEntity(DataType.US, us.toString());
                parsedData.setFile(file);
                return parsedData;
            })
            .toList();

        parsedDataRepository.saveAll(batch);
    }

    private String fetchStoryDescription(String storyId, String token) {
        try {
            JsonNode description = get("/userstories/{id}", token, storyId).get("description");
            return description != null ? description.asText() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private JsonNode get(String uri, String token, Map<String, String> extraHeaders, Object... uriVars) {
        return executeRequest(() -> {
            var spec = restClient.get()
                .uri(uri, uriVars)
                .header(HttpHeaders.AUTHORIZATION, bearer(token));

            extraHeaders.forEach(spec::header);

            return OBJECT_MAPPER.readTree(spec.retrieve().body(String.class));
        }, "Taiga GET request failed: " + uri);
    }

    private JsonNode get(String uri, String token, Object... uriVars) {
        return get(uri, token, Map.of(), uriVars);
    }

    private JsonNode post(String uri, Object body) {
        return executeRequest(() -> {
            String response = restClient.post()
                .uri(uri)
                .body(OBJECT_MAPPER.writeValueAsString(body))
                .retrieve()
                .body(String.class);
            return OBJECT_MAPPER.readTree(response);
        }, "Taiga POST request failed: " + uri);
    }

    private <T> T executeTaigaCall(Supplier<T> action, String unauthorizedMessage, String genericMessage) {
        try {
            return action.get();
        } catch (TaigaAuthException e) {
            throw e;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new TaigaAuthException(unauthorizedMessage);
        } catch (Exception e) {
            throw new TaigaException(genericMessage);
        }
    }

    private JsonNode executeRequest(RequestSupplier request, String errorMessage) {
        try {
            return request.get();
        } catch (HttpClientErrorException.Unauthorized e) {
            throw e;
        } catch (Exception e) {
            throw new TaigaException(errorMessage);
        }
    }

    @FunctionalInterface
    private interface RequestSupplier {
        JsonNode get() throws Exception;
    }

    private ArrayNode toArray(JsonNode root) {
        if (root == null) return null;
        if (root.isArray()) return (ArrayNode) root;
        JsonNode results = root.get("results");
        return (results != null && results.isArray()) ? (ArrayNode) results : null;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
