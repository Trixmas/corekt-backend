package lab.corekt.backend.modules.upload.taiga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import lab.corekt.backend.modules.upload.taiga.dto.*;
import lab.corekt.backend.modules.upload.taiga.service.HTTPService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.json.JsonUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/project/{projectId}/{versionId}/taiga")
class TaigaController {

    private final TaigaTokenService taigaTokenService;
    private final HTTPService httpService;
    private final ProjectRepository projectRepository;
    private final VersionRepository versionRepository;
    private final FileRepository fileRepository;
    private final AuthContext authContext;
    private final ParsedDataRepository parsedDataRepository;

    TaigaController(TaigaTokenService taigaTokenService, HTTPService httpService, ProjectRepository projectRepository, VersionRepository versionRepository, FileRepository fileRepository, AuthContext authContext, ParsedDataRepository parsedDataRepository) {
        this.taigaTokenService = taigaTokenService;
        this.httpService = httpService;
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.authContext = authContext;
        this.parsedDataRepository = parsedDataRepository;
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


    @Transactional
    @GetMapping("/us")
    public ResponseEntity<UsResponse> getUs(@Valid @PathVariable UUID projectId, @Valid @PathVariable Integer versionId, @Valid @RequestParam String taigaProjectId) {

        String taigaToken = taigaTokenService.getTaigaToken();

        UserEntity user = authContext.getCurrentUser();
        ProjectEntity dummyProject = projectRepository.save(new ProjectEntity("Test project", user));

        ProjectEntity project = projectRepository.findByIdAndOwner(dummyProject.getId(), user)
                .orElse(null);

        if (project == null) {
            return ResponseEntity.status(404).body(new UsResponse(null, false, "Project not found"));
        }

        VersionEntity version = versionRepository.findByIdAndProject(versionId, project)
                .orElse(null);
        if (version == null) {
            return ResponseEntity.status(404).body(new UsResponse(null, false, "Version not found"));
        }

        // Requête DB directe — évite le lazy loading de version.files
        FileEntity file = fileRepository.findByTypeAndVersion(FileType.US, version)
                .orElseGet(() -> {
                    FileEntity newFile = new FileEntity(FileType.US, "us.txt");
                    newFile.setVersion(version);
                    return fileRepository.save(newFile);
                });

        if (taigaToken == null) {
            return ResponseEntity.status(401).body(new UsResponse(null, false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            List<US> userStoriesResponse = new java.util.ArrayList<>();
            List<ParsedDataEntity> userStories = file.getParsedData();

            System.out.printf("Fetching user stories from Taiga (project: %s)%n", taigaProjectId);

            Map<String, String> headers = new HashMap<>();
            headers.put("x-disable-pagination", "True");

            String body = httpService.get("/userstories?project=" + taigaProjectId, taigaToken, headers);

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
                        US us = new US(subject, description);
                        userStoriesResponse.add(us);
                        ParsedDataEntity parsedDataEntity = new ParsedDataEntity(DataType.US, us.toString());
                        parsedDataEntity.setFile(file);
                        parsedDataRepository.save(parsedDataEntity);
                        userStories.add(parsedDataEntity);
                    } catch (Exception e) {
                        System.err.println("Error processing user story: " + e.getMessage());
                    }
                }
                file.setParsedData(userStories);
                fileRepository.save(file);
            }

            return ResponseEntity.ok(new UsResponse(userStoriesResponse));
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<UsResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = String.format("Invalid value '%s' for parameter '%s': expected %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown type");
        return ResponseEntity.badRequest().body(new UsResponse(null, false, msg));
    }

}
