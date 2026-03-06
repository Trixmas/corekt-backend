package lab.corekt.backend.modules.upload.taiga.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import lab.corekt.backend.modules.upload.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.upload.taiga.dto.US;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TaigaService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HTTPService httpService;
    private final FileService fileService;
    private final ParsedDataRepository parsedDataRepository;

    public TaigaService(HTTPService httpService, FileService fileService, ParsedDataRepository parsedDataRepository) {
        this.httpService = httpService;
        this.fileService = fileService;
        this.parsedDataRepository = parsedDataRepository;
    }

    /**
     * Authenticates against the Taiga API and returns the bearer token + user ID.
     *
     * @param username Taiga username
     * @param password Taiga password
     * @return credentials containing the token and Taiga user ID
     */
    public TaigaCredentials authenticate(String username, String password) throws Exception {
        String body = httpService.post(
                "/auth",
                String.format("{\"password\": \"%s\", \"type\": \"normal\", \"username\": \"%s\"}", password,
                        username));
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Invalid response from Taiga");
        }
        JsonNode root = OBJECT_MAPPER.readTree(body);
        JsonNode tokenNode = root.get("auth_token");
        JsonNode userIdNode = root.get("id");
        if (tokenNode == null || userIdNode == null) {
            throw new IllegalStateException("Invalid credentials or missing token");
        }
        return new TaigaCredentials(tokenNode.asText(), userIdNode.asText());
    }

    /**
     * Retrieves the list of Taiga projects the authenticated user is a member of.
     *
     * @param taigaToken  the authenticated bearer token
     * @param taigaUserId the Taiga user ID
     * @return a map of project name → project ID
     */
    public Map<String, String> fetchProjects(String taigaToken, String taigaUserId) throws Exception {
        String body = httpService.get("/projects?member=" + taigaUserId, taigaToken, null);
        JsonNode root = OBJECT_MAPPER.readTree(body);
        Map<String, String> projects = new LinkedHashMap<>();
        if (root.isArray()) {
            for (JsonNode n : root) {
                try {
                    projects.put(n.get("name").asText(), n.get("id").asText());
                } catch (Exception e) {
                    System.err.println("Error processing project: " + e.getMessage());
                }
            }
        }
        return projects;
    }

    /**
     * Fetches user stories from Taiga for the given project and persists them
     * as {@link ParsedDataEntity} records linked to the appropriate
     * {@link FileEntity}.
     *
     * @param taigaProjectId the Taiga project identifier
     * @param taigaToken     the authenticated Taiga bearer token
     * @param version        the version to associate the file / parsed data with
     * @return the list of fetched user stories
     */
    @Transactional
    public List<US> fetchAndSaveUserStories(String taigaProjectId, String taigaToken, VersionEntity version)
            throws Exception {
        FileEntity file = fileService.getOrCreate(FileType.US, "User Stories", version);

        Map<String, String> headers = new HashMap<>();
        headers.put("x-disable-pagination", "True");

        String body = httpService.get("/userstories?project=" + taigaProjectId, taigaToken, headers);
        ArrayNode results = parseResultsArray(body);

        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<US> userStoriesResponse = new ArrayList<>();
        List<ParsedDataEntity> parsedDataBatch = new ArrayList<>();

        for (JsonNode node : results) {
            try {
                String storyId = node.get("id").asText();
                String subject = node.get("subject").asText();
                String description = fetchUserStoryDescription(storyId, taigaToken);

                US us = new US(subject, description);
                userStoriesResponse.add(us);

                ParsedDataEntity parsedData = new ParsedDataEntity(DataType.US, us.toString());
                parsedData.setFile(file);
                parsedDataBatch.add(parsedData);
            } catch (Exception e) {
                System.err.println("Error processing user story: " + e.getMessage());
            }
        }

        parsedDataRepository.saveAll(parsedDataBatch);
        return userStoriesResponse;
    }


    // Helpers


    private ArrayNode parseResultsArray(String body) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        if (root.isArray()) {
            return (ArrayNode) root;
        }
        JsonNode resultsNode = root.get("results");
        return (resultsNode != null && resultsNode.isArray()) ? (ArrayNode) resultsNode : null;
    }

    private String fetchUserStoryDescription(String storyId, String taigaToken) {
        try {
            String res = httpService.get("/userstories/" + storyId, taigaToken, null);
            JsonNode descNode = OBJECT_MAPPER.readTree(res).get("description");
            return descNode != null ? descNode.asText() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
