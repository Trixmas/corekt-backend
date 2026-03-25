package lab.corekt.backend.modules.project.parsing.controller;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.dto.DataLinkResponse;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/analysis")
class AnalysisController {

    private final ProjectUtils projectUtils;
    private final VersionRepository versionRepository;
    private final DataLinkService dataLinkService;

    AnalysisController(ProjectUtils projectUtils,
                       VersionRepository versionRepository, DataLinkService dataLinkService) {
        this.projectUtils = projectUtils;
        this.versionRepository = versionRepository;
        this.dataLinkService = dataLinkService;
    }

    @GetMapping("/start")
    public ResponseEntity<List<DataLinkResponse>> buildAnalysis(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId).orElse(null);
        if (version == null) return ResponseEntity.notFound().build();

        List<DataLinkEntity> dataLinks = dataLinkService.saveDataLinks(version);

        List<DataLinkResponse> dataLinksResponse = dataLinks
                .stream()
                .map(DataLinkResponse::new)
                .toList();

        return ResponseEntity.ok(dataLinksResponse);
    }
}
