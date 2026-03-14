package lab.corekt.backend.modules.taiga.controller;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.taiga.dto.UserStoriesResponse;
import lab.corekt.backend.modules.taiga.service.TaigaService;
import lab.corekt.backend.modules.taiga.service.TaigaTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/user-stories")
    public ResponseEntity<UserStoriesResponse> getUserStories(
        @PathVariable UUID projectId,
        @PathVariable UUID versionId,
        @RequestParam String taigaProjectId) {
        try {
            ProjectEntity project = projectUtils.resolveProject(projectId);
            VersionEntity version = versionRepository.findByProjectAndId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

            return ResponseEntity.ok(new UserStoriesResponse(
                taigaService.fetchAndSaveUserStories(taigaProjectId, taigaTokenService.getToken(), version)
            ));
        } finally {
            taigaTokenService.remove();
        }
    }
}
