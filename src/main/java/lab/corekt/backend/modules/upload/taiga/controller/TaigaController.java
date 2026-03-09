package lab.corekt.backend.modules.upload.taiga.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.taiga.dto.*;
import lab.corekt.backend.modules.upload.taiga.service.TaigaService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/taiga")
class TaigaController {

    private final TaigaTokenService taigaTokenService;
    private final TaigaService taigaService;
    private final ProjectRepository projectRepository;
    private final VersionRepository versionRepository;
    private final AuthContext authContext;

    TaigaController(TaigaTokenService taigaTokenService, TaigaService taigaService, ProjectRepository projectRepository,
                    VersionRepository versionRepository, AuthContext authContext) {
        this.taigaTokenService = taigaTokenService;
        this.taigaService = taigaService;
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
        this.authContext = authContext;
    }

    //    TODO: refactor taiga login and projects route to not use /project/version endpoint to avoid useless project and version resolution
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginToTaiga(@Valid @RequestBody LoginRequest request) {
        try {
            TaigaCredentials credentials = taigaService.authenticate(request.getUsername(), request.getPassword());
            taigaTokenService.storeTaigaToken(credentials.token(), credentials.userId());
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
            return ResponseEntity.status(401)
                    .body(new ProjectResponse(false, "Not authenticated with Taiga. Please login first."));
        }

        try {
            Map<String, String> projects = taigaService.fetchProjects(taigaToken, taigaUserId);
            return ResponseEntity.ok(new ProjectResponse(projects));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ProjectResponse(false, "Failed to fetch projects from Taiga: " + e.getMessage()));
        }
    }

    @GetMapping("/us")
    public ResponseEntity<UsResponse> getUs(@Valid @PathVariable UUID projectId, @Valid @PathVariable UUID versionId,
                                            @Valid @RequestParam String taigaProjectId) {

        String taigaToken = taigaTokenService.getTaigaToken();
        if (taigaToken == null) {
            return ResponseEntity.status(401)
                    .body(new UsResponse(null, false, "Not authenticated with Taiga. Please login first."));
        }

        UserEntity user = authContext.getCurrentUser();
//TODO :refactor resolveProject
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, user).orElse(null);
        if (project == null) {
            return ResponseEntity.status(404).body(new UsResponse(null, false, "Project not found"));
        }

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId).orElse(null);
        if (version == null) {
            return ResponseEntity.status(404).body(new UsResponse(null, false, "Version not found"));
        }

        try {
            List<US> userStories = taigaService.fetchAndSaveUserStories(taigaProjectId, taigaToken, version);
            return ResponseEntity.ok(new UsResponse(userStories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new UsResponse(null, false, "Failed to fetch user stories from Taiga: " + e.getMessage()));
        } finally {
            taigaTokenService.removeTaigaToken();
        }
    }
}