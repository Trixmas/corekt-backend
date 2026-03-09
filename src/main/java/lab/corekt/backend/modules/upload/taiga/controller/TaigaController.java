package lab.corekt.backend.modules.upload.taiga.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.taiga.dto.US;
import lab.corekt.backend.modules.upload.taiga.dto.UsResponse;
import lab.corekt.backend.modules.upload.taiga.service.TaigaService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/taiga")
class TaigaController {

    private final TaigaTokenService taigaTokenService;
    private final TaigaService taigaService;
    private final VersionRepository versionRepository;
    private final ProjectUtils projectUtils;

    TaigaController(TaigaTokenService taigaTokenService, TaigaService taigaService,
                    VersionRepository versionRepository, ProjectUtils projectUtils) {
        this.taigaTokenService = taigaTokenService;
        this.taigaService = taigaService;
        this.versionRepository = versionRepository;
        this.projectUtils = projectUtils;
    }

    @GetMapping("/us")
    public ResponseEntity<UsResponse> getUs(@Valid @PathVariable UUID projectId, @Valid @PathVariable UUID versionId,
                                            @Valid @RequestParam String taigaProjectId) {

        String taigaToken = taigaTokenService.getTaigaToken();
        if (taigaToken == null) {
            return ResponseEntity.status(401)
                    .body(new UsResponse(null, false, "Not authenticated with Taiga. Please login first."));
        }

        ProjectEntity project = projectUtils.resolveProject(projectId);
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