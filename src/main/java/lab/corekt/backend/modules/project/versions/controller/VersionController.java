package lab.corekt.backend.modules.project.versions.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions")
public class VersionController {
    private final VersionService versionService;
    private final ProjectUtils projectUtils;

    public VersionController(VersionService versionService, ProjectUtils projectUtils) {
        this.versionService = versionService;
        this.projectUtils = projectUtils;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getAllVersions(@PathVariable UUID projectId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getAllByProject(project));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionDetailsResponse> getVersion(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getOneVersion(project, versionId));
    }


    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID projectId,
            @Valid @RequestBody VersionRequest request
    ) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.createVersion(project, request));
    }

    @PatchMapping("/{versionId}")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @RequestBody VersionRequest request
    ) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.updateVersion(project, versionId, request));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        versionService.deleteVersion(project, versionId);
        return ResponseEntity.noContent().build();
    }

}