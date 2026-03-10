package lab.corekt.backend.modules.project.controller;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.service.ProjectService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private AuthContext authContext;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProjectUtils projectUtils;

    private UserEntity buildUser() {
        return new UserEntity("user@test.com", "testuser");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with list of projects when user is authenticated")
    void testReturns200WithProjectListWhenUserIsAuthenticated() throws Exception {
        UserEntity user = buildUser();
        List<ProjectEntity> projects = List.of(
                new ProjectEntity("Project A", "Desc A", user),
                new ProjectEntity("Project B", "Desc B", user)
        );

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectService.getAllOwnerProjects(user)).thenReturn(projects);

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Project A"))
                .andExpect(jsonPath("$[1].title").value("Project B"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 201 when project is created successfully")
    void testReturns201WhenProjectIsCreatedSuccessfully() throws Exception {
        UserEntity user = buildUser();
        ProjectEntity project = new ProjectEntity("New Project", "A description", user);

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectService.createProject(any(), eq(user))).thenReturn(project);

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"title": "New Project", "description": "A description"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Project"))
                .andExpect(jsonPath("$.description").value("A description"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when create request is missing title")
    void testReturns400WhenCreateRequestIsMissingTitle() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"description": "A description"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with project when it belongs to owner")
    void testReturns200WithProjectWhenItBelongsToOwner() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity project = new ProjectEntity("My Project", "A description", user);

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectUtils.resolveProject(projectId)).thenReturn(project);

        mockMvc.perform(get("/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Project"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when project does not belong to owner")
    void testReturns404WhenProjectDoesNotBelongToOwner() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectUtils.resolveProject(projectId)).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}", projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 201 when project is updated successfully")
    void testReturns201WhenProjectIsUpdatedSuccessfully() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();
        ProjectEntity updatedProject = new ProjectEntity("Updated Title", "Updated Description", user);

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectService.updateProject(eq(projectId), any(), eq(user))).thenReturn(updatedProject);

        mockMvc.perform(patch("/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"title": "Updated Title", "description": "Updated Description"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when updating project not owned by user")
    void testReturns404WhenUpdatingProjectNotOwnedByUser() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectService.updateProject(eq(projectId), any(), eq(user)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"title": "Updated Title", "description": "Updated Description"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 204 when project is deleted successfully")
    void testReturns204WhenProjectIsDeletedSuccessfully() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(authContext.getCurrentUser()).thenReturn(user);
        doNothing().when(projectService).deleteProject(projectId, user);

        mockMvc.perform(delete("/projects/{projectId}", projectId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when deleting project not owned by user")
    void testReturns404WhenDeletingProjectNotOwnedByUser() throws Exception {
        UserEntity user = buildUser();
        UUID projectId = UUID.randomUUID();

        when(authContext.getCurrentUser()).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(projectService).deleteProject(projectId, user);

        mockMvc.perform(delete("/projects/{projectId}", projectId))
                .andExpect(status().isNotFound());
    }
}
