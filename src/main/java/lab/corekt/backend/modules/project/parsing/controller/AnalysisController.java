package lab.corekt.backend.modules.project.parsing.controller;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/analysis")
class AnalysisController {

    private final ProjectUtils projectUtils;
    private final VersionRepository versionRepository;
    private final DataLinkService dataLinkService;

    AnalysisController(ProjectUtils projectUtils, ParsedDataRepository parsedDataRepository, MistralService mistralService,
                       VersionRepository versionRepository, DataLinkService dataLinkService) {
        this.projectUtils = projectUtils;
        this.versionRepository = versionRepository;
        this.dataLinkService = dataLinkService;
    }

    @GetMapping("/start")
    public ResponseEntity<List<DataLinkResponse>> buildAnalysis(@PathVariable UUID projectId, @PathVariable UUID versionId){
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) return ResponseEntity.notFound().build();

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId).orElse(null);
        if (version == null) return ResponseEntity.notFound().build();

        List<DataLinkEntity> dataLinks = dataLinkService.buildDataLinks(version);

        List<DataLinkResponse> dataLinksResponse = dataLinks
                .stream()
                .map(dataLink -> new DataLinkResponse(
                        dataLink.getId(),
                        dataLink.getMatch(),
                        dataLink.getSeverity(),
                        dataLink.getConfidenceScore(),
                        dataLink.getScore(),
                        dataLink.getJustification(),
                        dataLink.getSuggestion(),
                        new ParsedDataResponse(
                                dataLink.getSourceParsedData().getId(),
                                dataLink.getSourceParsedData().getDataType(),
                                dataLink.getSourceParsedData().getContent(),
                                dataLink.getSourceParsedData().getFile().getId(),
                                dataLink.getSourceParsedData().getFile().getName(),
                                dataLink.getSourceParsedData().getFile().getType()),
                        new ParsedDataResponse(
                                dataLink.getTargetParsedData().getId(),
                                dataLink.getTargetParsedData().getDataType(),
                                dataLink.getTargetParsedData().getContent(),
                                dataLink.getTargetParsedData().getFile().getId(),
                                dataLink.getTargetParsedData().getFile().getName(),
                                dataLink.getTargetParsedData().getFile().getType())))
                .toList();

        dataLinkService.saveDataLinks(version);
        return ResponseEntity.ok(dataLinksResponse);
    }

    record DataLinkResponse(
            UUID id,
            MatchType match,
            Severity severity,
            Integer confidenceScore,
            Integer score,
            String justification,
            String suggestion,
            ParsedDataResponse sourceParsedData,
            ParsedDataResponse targetParsedData) {
    }

    record ParsedDataResponse(
            UUID parsedDataId,
            DataType dataType,
            String content,
            UUID fileId,
            String fileName,
            FileType fileType) {
    }
}
