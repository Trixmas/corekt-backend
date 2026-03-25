package lab.corekt.backend.modules.project.versions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionIssuesResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = VersionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VersionControllerTest extends BaseControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private ProjectUtils projectUtils;
    @MockitoBean
    private VersionService versionService;
    private UserEntity currentUser;
    private ProjectEntity project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        currentUser = new UserEntity("user@example.com", "testUser");
        currentUser.setId(UUID.randomUUID());

        projectId = UUID.randomUUID();
        project = new ProjectEntity("Test Project", "A description", currentUser);
        project.setId(projectId);

        when(projectUtils.resolveProject(projectId)).thenReturn(project);
    }

    private VersionResponse buildVersionResponse(UUID id, int versionNumber, String title) {
        VersionResponse response = new VersionResponse();
        response.setId(id);
        response.setVersion(versionNumber);
        response.setTitle(title);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setIssues(new VersionIssuesResponse(0, 0, 0, 0));
        return response;
    }

    private VersionDetailsResponse buildVersionDetailsResponse(UUID id, int versionNumber, String title) {
        VersionDetailsResponse response = new VersionDetailsResponse();
        response.setId(id);
        response.setVersion(versionNumber);
        response.setTitle(title);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        response.setIssues(new VersionIssuesResponse(0, 0, 0, 0));
        return response;
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions returns 200 with list of versions")
    void testGetProjectVersionsReturns200() throws Exception {
        UUID v1Id = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();
        VersionResponse v1 = buildVersionResponse(v1Id, 1, "v1");
        VersionResponse v2 = buildVersionResponse(v2Id, 2, "v2");

        when(versionService.getAllByProject(eq(project)))
                .thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/projects/{projectId}/versions", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].title").value("v1"))
                .andExpect(jsonPath("$[1].version").value(2))
                .andExpect(jsonPath("$[1].title").value("v2"));
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions returns 200 with empty list when project has no versions")
    void testGetProjectVersionsReturnsEmptyList() throws Exception {
        when(versionService.getAllByProject(eq(project)))
                .thenReturn(List.of());

        mockMvc.perform(get("/projects/{projectId}/versions", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions returns 404 when project not found")
    void testGetProjectVersionsReturns404WhenProjectNotFound() throws Exception {
        when(projectUtils.resolveProject(any())).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}/versions", projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 200 with version details")
    void testGetProjectVersionReturns200() throws Exception {
        UUID versionId = UUID.randomUUID();
        VersionDetailsResponse v1 = buildVersionDetailsResponse(versionId, 1, "Initial version");

        when(versionService.getVersion(eq(project), eq(versionId)))
                .thenReturn(v1);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(versionId.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.title").value("Initial version"))
                .andExpect(jsonPath("$.issues").exists())
                .andExpect(jsonPath("$.dataLinks").exists())
                .andExpect(jsonPath("$.dataLinks.length()").value(0));

    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 404 when version not found")
    void testGetProjectVersionReturns404WhenVersionNotFound() throws Exception {
        UUID versionId = UUID.randomUUID();

        when(versionService.getVersion(eq(project), eq(versionId)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Version not found"));

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 404 when project not found")
    void testGetProjectVersionReturns404WhenProjectNotFound() throws Exception {
        when(projectUtils.resolveProject(any())).thenReturn(null);

        mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/versions returns 200 with created version")
    void testCreateVersionReturns200() throws Exception {
        UUID versionId = UUID.randomUUID();
        VersionResponse created = buildVersionResponse(versionId, 2, "Sprint 2");

        when(versionService.createVersion(eq(project), any(VersionRequest.class)))
                .thenReturn(created);

        VersionRequest request = new VersionRequest();
        request.setTitle("Sprint 2");

        mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(versionId.toString()))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.title").value("Sprint 2"))
                .andExpect(jsonPath("$.issues").exists());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/versions returns 404 when project not found")
    void testCreateVersionReturns404WhenProjectNotFound() throws Exception {
        when(projectUtils.resolveProject(any())).thenReturn(null);

        VersionRequest request = new VersionRequest();
        request.setTitle("Sprint 2");

        mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/versions returns 400 when title is blank")
    void testCreateVersionReturns400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/versions returns 400 when title exceeds 50 characters")
    void testCreateVersionReturns400WhenTitleTooLong() throws Exception {
        String longTitle = "A".repeat(51);

        mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /projects/{projectId}/versions returns 400 when body is missing")
    void testCreateVersionReturns400WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /projects/{projectId}/versions/{versionId} returns 200 with updated version")
    void testUpdateVersionReturns200() throws Exception {
        UUID versionId = UUID.randomUUID();
        VersionResponse updated = buildVersionResponse(versionId, 1, "Updated title");

        when(versionService.updateVersion(eq(project), eq(versionId), any(VersionRequest.class)))
                .thenReturn(updated);

        VersionRequest request = new VersionRequest();
        request.setTitle("Updated title");

        mockMvc.perform(patch("/projects/{projectId}/versions/{versionId}", projectId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(versionId.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    @DisplayName("PATCH /projects/{projectId}/versions/{versionId} returns 404 when project not found")
    void testUpdateVersionReturns404WhenProjectNotFound() throws Exception {
        when(projectUtils.resolveProject(any())).thenReturn(null);

        VersionRequest request = new VersionRequest();
        request.setTitle("Updated title");

        mockMvc.perform(patch("/projects/{projectId}/versions/{versionId}", projectId, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /projects/{projectId}/versions/{versionId} returns 404 when version not found")
    void testUpdateVersionReturns404WhenVersionNotFound() throws Exception {
        UUID versionId = UUID.randomUUID();

        when(versionService.updateVersion(eq(project), eq(versionId), any(VersionRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Version not found"));

        VersionRequest request = new VersionRequest();
        request.setTitle("Updated title");

        mockMvc.perform(patch("/projects/{projectId}/versions/{versionId}", projectId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /projects/{projectId}/versions/{versionId} ignores null title and returns 200")
    void testUpdateVersionIgnoresNullTitle() throws Exception {
        UUID versionId = UUID.randomUUID();
        VersionResponse unchanged = buildVersionResponse(versionId, 1, "Original title");

        when(versionService.updateVersion(eq(project), eq(versionId), any(VersionRequest.class)))
                .thenReturn(unchanged);

        mockMvc.perform(patch("/projects/{projectId}/versions/{versionId}", projectId, versionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Original title"));
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId} returns 204 when version is deleted")
    void testDeleteVersionReturns204() throws Exception {
        UUID versionId = UUID.randomUUID();

        doNothing().when(versionService).deleteVersion(eq(project), eq(versionId));

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                .andExpect(status().isNoContent());

        verify(versionService, times(1)).deleteVersion(eq(project), eq(versionId));
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId} returns 404 when project not found")
    void testDeleteVersionReturns404WhenProjectNotFound() throws Exception {
        when(projectUtils.resolveProject(any())).thenReturn(null);

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}", projectId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/versions/{versionId} returns 404 when version not found")
    void testDeleteVersionReturns404WhenVersionNotFound() throws Exception {
        UUID versionId = UUID.randomUUID();

        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Version not found"))
                .when(versionService).deleteVersion(eq(project), eq(versionId));

        mockMvc.perform(delete("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                .andExpect(status().isNotFound());
    }
}
