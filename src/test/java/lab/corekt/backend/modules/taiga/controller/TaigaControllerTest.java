package lab.corekt.backend.modules.taiga.controller;

import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.taiga.dto.UserStory;
import lab.corekt.backend.modules.taiga.exception.TaigaException;
import lab.corekt.backend.modules.taiga.service.TaigaService;
import lab.corekt.backend.modules.taiga.service.TaigaTokenService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaigaController.class)
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

        UserEntity owner = new UserEntity("alice@example.com", "alice");
        owner.setId(UUID.randomUUID());

        project = new ProjectEntity("Corekt", "Test project", owner);
        project.setId(projectId);

        version = new VersionEntity(project, "Version 1");
        version.setId(versionId);
        version.setVersionNumber(1);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/user-stories returns imported stories")
    void testGetUserStoriesReturnsImportedStories() throws Exception {
        List<UserStory> stories = List.of(
            new UserStory("US 1", "Desc 1"),
            new UserStory("US 2", "Desc 2")
        );

        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(taigaService.fetchAndSaveUserStories("12", "token-123", version)).thenReturn(stories);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/user-stories", projectId, versionId)
                .param("taigaProjectId", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userStories.length()").value(2))
            .andExpect(jsonPath("$.userStories[0].title").value("US 1"))
            .andExpect(jsonPath("$.userStories[1].description").value("Desc 2"));

        verify(taigaTokenService).remove();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/user-stories returns 404 when version is missing")
    void testGetUserStoriesReturns404WhenVersionIsMissing() throws Exception {
        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/user-stories", projectId, versionId)
                .param("taigaProjectId", "12"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Version not found"));

        verify(taigaTokenService).remove();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/user-stories returns 404 when project is missing")
    void testGetUserStoriesReturns404WhenProjectIsMissing() throws Exception {
        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(projectUtils.resolveProject(projectId))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Project not found"));

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/user-stories", projectId, versionId)
                .param("taigaProjectId", "12"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found"));

        verify(taigaTokenService).remove();
    }

    @Test
    @WithMockUser
    @DisplayName("GET /projects/{projectId}/versions/{versionId}/taiga/user-stories returns 502 when Taiga import fails")
    void testGetUserStoriesReturns502WhenImportFails() throws Exception {
        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(projectUtils.resolveProject(projectId)).thenReturn(project);
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(taigaService.fetchAndSaveUserStories("12", "token-123", version))
            .thenThrow(new TaigaException("Failed to fetch user stories from Taiga"));

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}/taiga/user-stories", projectId, versionId)
                .param("taigaProjectId", "12"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message").value("Failed to fetch user stories from Taiga"));

        verify(taigaTokenService).remove();
    }
}
