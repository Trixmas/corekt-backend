package lab.corekt.backend.modules.project.versions.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.versions.dto.VersionCreateRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionUpdateRequest;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/projects/{projectId}/versions")
public class VersionController {
    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getVersions(@PathVariable UUID projectId) {
        return ResponseEntity.ok(versionService.getAllByProjectId(projectId));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionResponse> getVersion(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        return ResponseEntity.ok(versionService.getOneVersion(projectId, versionId));
    }

    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID projectId,
            @Valid @RequestBody VersionCreateRequest request
    ) {
        return ResponseEntity.ok(versionService.createVersion(projectId, request));
    }

    @PatchMapping("/{versionId}")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @RequestBody VersionUpdateRequest request
    ) {
        return ResponseEntity.ok(versionService.updateVersion(projectId, versionId, request));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<VersionResponse> deleteVersion(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        versionService.deleteVersion(projectId, versionId);
        return ResponseEntity.noContent().build();
    }
}
