package lab.corekt.backend.modules.project.parsing.controller;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/analysis")
@AllArgsConstructor
class AnalysisController {

    private final ProjectUtils projectUtils;
    private final VersionRepository versionRepository;
    private final DataLinkService dataLinkService;
    private final VersionService versionService;
    private final ParsedDataRepository parsedDataRepository;
    private final DataLinkRepository dataLinkRepository;

    @GetMapping("/start")
    public ResponseEntity<List<DataLinkResponse>> buildAnalysis(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId).orElse(null);
        if (version == null) return ResponseEntity.notFound().build();

        versionService.setVersionStatus(version, VersionStatus.IN_PROGRESS);

        try {
            List<DataLinkEntity> dataLinks = dataLinkService.saveDataLinks(version);
            List<DataLinkResponse> dataLinksResponse = dataLinks
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

    @DeleteMapping("/datalink/{datalinkId}")
    public ResponseEntity<Void> deleteDataLink(@PathVariable UUID projectId, @PathVariable UUID versionId, @PathVariable UUID datalinkId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId).orElse(null);
        if (version == null) return ResponseEntity.notFound().build();

        dataLinkRepository.deleteByIdAndVersionId(datalinkId, version.getId());
        return ResponseEntity.noContent().build();
    }
}
