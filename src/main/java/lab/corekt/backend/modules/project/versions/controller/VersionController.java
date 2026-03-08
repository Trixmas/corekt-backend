package lab.corekt.backend.modules.project.versions.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions")
public class VersionController {
    private final VersionService versionService;
    private final AuthContext authContext;
    private final ProjectRepository projectRepository;

    public VersionController(VersionService versionService, AuthContext authContext, ProjectRepository projectRepository) {
        this.versionService = versionService;
        this.authContext = authContext;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getAllVersions(@PathVariable ProjectEntity project, @PathVariable UserEntity user) {
        if (resolveProject(project.getId()) == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getAllByProject(project, user));
    }

    private ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        return projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionResponse> getVersion(@PathVariable ProjectEntity project, @PathVariable UUID versionId) {
        if (resolveProject(project.getId()) == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getOneVersion(project, versionId));
    }

    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable ProjectEntity project,
            @Valid @RequestBody VersionRequest request
    ) {
        if (resolveProject(project.getId()) == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.createVersion(project, request));
    }

    @PatchMapping("/{versionId}")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable ProjectEntity project,
            @PathVariable UUID versionId,
            @RequestBody VersionRequest request
    ) {
        if (resolveProject(project.getId()) == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.updateVersion(project, versionId, request));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<VersionResponse> deleteVersion(@PathVariable ProjectEntity project, @PathVariable UUID versionId) {
        if (resolveProject(project.getId()) == null) return ResponseEntity.notFound().build();

        versionService.deleteVersion(project, versionId);
        return ResponseEntity.noContent().build();
    }
}
