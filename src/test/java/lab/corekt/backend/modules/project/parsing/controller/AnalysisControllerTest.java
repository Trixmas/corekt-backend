package lab.corekt.backend.modules.project.parsing.controller;

import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.service.AnalysisService;
import lab.corekt.backend.modules.project.parsing.service.DataLinkService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AnalysisController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AnalysisControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectUtils projectUtils;

    @MockitoBean
    private VersionRepository versionRepository;

    @MockitoBean
    private DataLinkService dataLinkService;

    @MockitoBean
    private VersionService versionService;

    @MockitoBean
    private MistralService mistralService;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private JwtService jwtService;


    private UUID projectId;
    private UUID versionId;
    private ProjectEntity project;
    private VersionEntity version;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        UserEntity owner = new UserEntity("user@example.com", "testUser");
        owner.setId(UUID.randomUUID());

        project = new ProjectEntity("Test Project", "A description", owner);
        project.setId(projectId);

        version = new VersionEntity(project, "Version 1");
        version.setId(versionId);
        version.setVersionNumber(1);
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/analysis/start returns 200 with analysis payload")
    void testBuildAnalysisReturns200WithMappedDataLinks() throws Exception {
        FileEntity sourceFile = new FileEntity(FileType.BPMN, "diagram.bpmn");
        sourceFile.setVersion(version);
        FileEntity targetFile = new FileEntity(FileType.INTERVIEW, "interview.pdf");
        targetFile.setVersion(version);

        ParsedDataEntity sourceParsedData = new ParsedDataEntity(DataType.ACTOR, "Customer", sourceFile);
        ParsedDataEntity targetParsedData = new ParsedDataEntity(DataType.ACTOR, "Client", targetFile);

        DataLinkEntity dataLink = new DataLinkEntity(MatchType.SEMANTIC, Severity.MINOR, sourceParsedData, targetParsedData);
        UUID dataLinkId = UUID.randomUUID();
        dataLink.setId(dataLinkId);
        dataLink.setConfidenceScore(87);
        dataLink.setJustification("Semantic similarity found");
        dataLink.setSuggestion("Rename one of the actors");

        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(dataLinkService.saveDataLinks(version)).thenReturn(List.of(dataLink));

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/analysis/start", projectId, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(dataLinkId.toString()))
                .andExpect(jsonPath("$[0].match").value("SEMANTIC"))
                .andExpect(jsonPath("$[0].severity").value("MINOR"))
                .andExpect(jsonPath("$[0].confidenceScore").value(87))
                .andExpect(jsonPath("$[0].justification").value("Semantic similarity found"))
                .andExpect(jsonPath("$[0].suggestion").value("Rename one of the actors"))
                .andExpect(jsonPath("$[0].sourceParsedData.fileName").value("diagram.bpmn"))
                .andExpect(jsonPath("$[0].sourceParsedData.fileType").value("BPMN"))
                .andExpect(jsonPath("$[0].targetParsedData.fileName").value("interview.pdf"))
                .andExpect(jsonPath("$[0].targetParsedData.fileType").value("INTERVIEW"));

        verify(dataLinkService).saveDataLinks(version);
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/analysis/start returns 404 when project is missing")
    void testBuildAnalysisReturns404WhenProjectIsMissing() throws Exception {
        when(projectUtils.resolveProject(projectId)).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/analysis/start", projectId, versionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/analysis/start returns 404 when version is missing")
    void testBuildAnalysisReturns404WhenVersionIsMissing() throws Exception {
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/analysis/start", projectId, versionId))
                .andExpect(status().isNotFound());
    }

    // ============ DELETE DATA LINK ENDPOINT TESTS ============

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId} returns 204 on successful deletion")
    void testDeleteDataLinkReturns204OnSuccess() throws Exception {
        UUID dataLinkId = UUID.randomUUID();
        doNothing().when(analysisService).deleteDataLink(projectId, versionId, dataLinkId);

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId}",
                        projectId, versionId, dataLinkId))
                .andExpect(status().isNoContent());

        verify(analysisService).deleteDataLink(projectId, versionId, dataLinkId);
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId} returns 404 when project is not found")
    void testDeleteDataLinkReturns404WhenProjectNotFound() throws Exception {
        UUID dataLinkId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"))
                .when(analysisService).deleteDataLink(projectId, versionId, dataLinkId);

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId}",
                        projectId, versionId, dataLinkId))
                .andExpect(status().isNotFound());

        verify(analysisService).deleteDataLink(projectId, versionId, dataLinkId);
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId} returns 404 when version is not found")
    void testDeleteDataLinkReturns404WhenVersionNotFound() throws Exception {
        UUID dataLinkId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"))
                .when(analysisService).deleteDataLink(projectId, versionId, dataLinkId);

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId}",
                        projectId, versionId, dataLinkId))
                .andExpect(status().isNotFound());

        verify(analysisService).deleteDataLink(projectId, versionId, dataLinkId);
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId} returns 404 when data link is not found")
    void testDeleteDataLinkReturns404WhenDataLinkNotFound() throws Exception {
        UUID dataLinkId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Data link not found"))
                .when(analysisService).deleteDataLink(projectId, versionId, dataLinkId);

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}/analysis/datalink/{datalinkId}",
                        projectId, versionId, dataLinkId))
                .andExpect(status().isNotFound());

        verify(analysisService).deleteDataLink(projectId, versionId, dataLinkId);
    }
}
