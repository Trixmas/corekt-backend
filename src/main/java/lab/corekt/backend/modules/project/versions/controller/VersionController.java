package lab.corekt.backend.modules.project.versions.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.CreateVersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/project/{projectId}/versions")
class VersionController {

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final AuthContext authContext;
    private final VersionService versionService;

    VersionController(VersionRepository versionRepository, ProjectRepository projectRepository,
                      AuthContext authContext, VersionService versionService) {
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.authContext = authContext;
        this.versionService = versionService;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getProjectVersions(@PathVariable UUID projectId) {
        ProjectEntity project = resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        List<VersionResponse> versionResponses = versionRepository.findAllByProject(project)
                .stream()
                .map(VersionResponse::new)
                .toList();

        return ResponseEntity.ok(versionResponses);
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionResponse> getProjectVersion(
            @PathVariable UUID projectId,
            @PathVariable Integer versionId) {
        ProjectEntity project = resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return versionRepository.findByIdAndProject(versionId, project)
                .map(VersionResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateVersionRequest request) {
        ProjectEntity project = resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        VersionEntity version = versionService.createVersion(project, request.getTitle());
        return ResponseEntity.status(201).body(new VersionResponse(version));
    }

    private ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        return projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }

}
