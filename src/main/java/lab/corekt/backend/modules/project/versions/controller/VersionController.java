package lab.corekt.backend.modules.project.versions.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.service.VersionPdfExportService;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/versions")
@AllArgsConstructor
public class VersionController {
    private final VersionService versionService;
    private final VersionPdfExportService versionPdfExportService;
    private final ProjectUtils projectUtils;

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getAllVersions(@PathVariable UUID projectId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getAllByProject(project));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionDetailsResponse> getVersion(@PathVariable UUID projectId,
            @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.getVersion(project, versionId));
    }

    @GetMapping("/{versionId}/export/pdf")
    public ResponseEntity<byte[]> exportVersionPdf(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        byte[] pdf = versionPdfExportService.generateVersionSummaryPdf(project, versionId);
        String filename = "corekt-version-" + versionId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable UUID projectId,
            @Valid @RequestBody VersionRequest request) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.createVersion(project, request));
    }

    @PatchMapping("/{versionId}")
    public ResponseEntity<VersionResponse> updateVersion(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @RequestBody VersionRequest request) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(versionService.updateVersion(project, versionId, request));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        versionService.deleteVersion(project, versionId);
        return ResponseEntity.noContent().build();
    }
}
