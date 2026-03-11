package lab.corekt.backend.modules.upload.taiga.controller;

import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.taiga.dto.US;
import lab.corekt.backend.modules.upload.taiga.service.TaigaService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value =  TaigaController.class, excludeAutoConfiguration =  SecurityAutoConfiguration.class)
class TaigaControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TaigaTokenService taigaTokenService;

    @MockitoBean
    private TaigaService taigaService;

    @MockitoBean
    private VersionRepository versionRepository;

    @MockitoBean
    private ProjectUtils projectUtils;

    private UUID projectId;
    private UUID versionId;
    private ProjectEntity project;
    private VersionEntity version;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        UserEntity user = new UserEntity("alice@example.com", "alice");
        user.setId(UUID.randomUUID());

        project = new ProjectEntity("Corekt", "Test project", user);
        project.setId(projectId);

        version = new VersionEntity(project, "Version 1");
        version.setId(versionId);
        version.setVersionNumber(1);
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/us returns 401 when no Taiga token")
    void getUs_returns401_whenNoTaigaToken() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/us", projectId, versionId)
                .param("taigaProjectId", "12"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Not authenticated with Taiga")));

        verify(projectUtils, never()).resolveProject(any());
        verify(taigaTokenService, never()).removeTaigaToken();
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionsId}/taiga/us returns 404 when project is missing")
    void getUs_returns404_whenProjectMissing() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn("token");
        when(projectUtils.resolveProject(projectId)).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/us", projectId, versionId)
                    .param("taigaProjectId", "12"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Project not found")));

        verify(taigaTokenService, never()).removeTaigaToken();
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/us returns 404 when version is missing")
    void getUs_returns404_whenVersionMissing() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn("token");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project,  versionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/us", projectId, versionId)
                    .param("taigaProjectId", "12"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Version not found")));

        verify(taigaTokenService, never()).removeTaigaToken();
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/us returns 200 with imported US")
    void getUs_returns200_withImportSucceeds() throws Exception {
        List<US> stories = List.of(new US("US 1", "Desc 1"), new US("US 2", "Desc 2"));

        when(taigaTokenService.getTaigaToken()).thenReturn("token");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project,  versionId)).thenReturn(Optional.of(version));
        when(taigaService.fetchAndSaveUserStories("12", "token", version)).thenReturn(stories);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/us", projectId, versionId)
                    .param("taigaProjectId", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("US 1")))
                .andExpect(content().string(containsString("US 2")));

        verify(taigaTokenService).removeTaigaToken();
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/us returns 500 when import fails")
    void getUs_returns500_whenImportFails() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn("token");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project,  versionId)).thenReturn(Optional.of(version));
        when(taigaService.fetchAndSaveUserStories("12", "token", version)).thenThrow(new RuntimeException("Taiga error"));

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/us", projectId, versionId)
                    .param("taigaProjectId", "12"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to fetch user stories from Taiga")))
                .andExpect(content().string(containsString("Taiga error")));

        verify(taigaTokenService).removeTaigaToken();
    }
}