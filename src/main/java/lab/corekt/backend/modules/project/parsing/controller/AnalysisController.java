package lab.corekt.backend.modules.project.parsing.controller;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.parsing.service.AnalysisService;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing project analysis and data link operations.
 * Handles building analysis for project versions and managing data links.
 */
@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/analysis")
@AllArgsConstructor
public class AnalysisController {

    private final ProjectUtils projectUtils;
    private final VersionRepository versionRepository;
    private final DataLinkService dataLinkService;
    private final VersionService versionService;
    private final AnalysisService analysisService;

    /**
     * Initiates and builds analysis for a project version.
     * Updates the version status through the analysis process:
     * IN_PROGRESS -> COMPLETED (success) or FAILED (error)
     *
     * @param projectId the UUID of the project
     * @param versionId the UUID of the version
     * @return list of generated data links
     * @throws ResponseStatusException if project or version not found
     */
    @GetMapping("/start")
    public ResponseEntity<List<DataLinkResponse>> buildAnalysis(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId
    ) {
        VersionEntity version = resolveVersion(projectId, versionId);
        versionService.setVersionStatus(version, VersionStatus.IN_PROGRESS);

        try {
            List<DataLinkResponse> dataLinksResponse = dataLinkService.saveDataLinks(version)
                    .stream()
                    .map(DataLinkResponse::new)
                    .toList();

            versionService.setVersionStatus(version, VersionStatus.COMPLETED);
            return ResponseEntity.ok(dataLinksResponse);
        } catch (Exception e) {
            versionService.setVersionStatus(version, VersionStatus.FAILED);
            throw e;
        }
    }

    /**
     * Deletes a specific data link from a project version.
     *
     * @param projectId  the UUID of the project
     * @param versionId  the UUID of the version
     * @param datalinkId the UUID of the data link to delete
     * @return no content response on success
     * @throws ResponseStatusException if project, version, or data link not found
     */
    @DeleteMapping("/datalink/{datalinkId}")
    public ResponseEntity<Void> deleteDataLink(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @PathVariable UUID datalinkId
    ) {
        analysisService.deleteDataLink(projectId, versionId, datalinkId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resolves a version by project and version IDs.
     *
     * @param projectId the UUID of the project
     * @param versionId the UUID of the version
     * @return the resolved VersionEntity
     * @throws ResponseStatusException if project or version not found
     */
    private VersionEntity resolveVersion(UUID projectId, UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        return versionRepository
                .findByProjectAndId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }
}
