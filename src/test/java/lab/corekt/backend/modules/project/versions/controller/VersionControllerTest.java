package lab.corekt.backend.modules.project.versions.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionIssuesResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.service.VersionService;
import lab.corekt.backend.modules.user.entity.UserEntity;

@WebMvcTest(value = VersionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VersionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private ProjectRepository projectRepository;

        @MockitoBean
        private AuthContext authContext;

        @MockitoBean
        private VersionService versionService;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private UserEntity currentUser;
        private ProjectEntity project;
        private UUID projectId;

        @BeforeEach
        void setUp() {
                currentUser = new UserEntity("user@example.com", "testUser");
                currentUser.setId(UUID.randomUUID());
                when(authContext.getCurrentUser()).thenReturn(currentUser);

                projectId = UUID.randomUUID();
                project = new ProjectEntity("Test Project", "A description", currentUser);
                project.setId(projectId);
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

        @Test
        @DisplayName("GET /projects/{projectId}/versions returns 200 with version list")
        void testGetProjectVersionsReturns200() throws Exception {
                UUID v1Id = UUID.randomUUID();
                UUID v2Id = UUID.randomUUID();
                VersionResponse v1 = buildVersionResponse(v1Id, 1, "v1");
                VersionResponse v2 = buildVersionResponse(v2Id, 2, "v2");

                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));
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
                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));
                when(versionService.getAllByProject(eq(project)))
                        .thenReturn(List.of());

                mockMvc.perform(get("/projects/{projectId}/versions", projectId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("GET /projects/{projectId}/versions returns 404 when project not found")
        void testGetProjectVersionsReturns404WhenProjectNotFound() throws Exception {
                when(projectRepository.findByIdAndOwner(any(), eq(currentUser)))
                        .thenReturn(Optional.empty());

                mockMvc.perform(get("/projects/{projectId}/versions", projectId))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 200 with version details")
        void testGetProjectVersionReturns200() throws Exception {
                UUID versionId = UUID.randomUUID();
                VersionResponse v1 = buildVersionResponse(versionId, 1, "Initial version");

                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));
                when(versionService.getOneVersion(eq(project), eq(versionId)))
                        .thenReturn(v1);

                mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.version").value(1))
                        .andExpect(jsonPath("$.title").value("Initial version"));
        }

        @Test
        @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 404 when version not found")
        void testGetProjectVersionReturns404WhenVersionNotFound() throws Exception {
                UUID versionId = UUID.randomUUID();

                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));
                when(versionService.getOneVersion(eq(project), eq(versionId)))
                        .thenThrow(new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND, "Version not found"));

                mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, versionId))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /projects/{projectId}/versions/{versionId} returns 404 when project not found")
        void testGetProjectVersionReturns404WhenProjectNotFound() throws Exception {
                when(projectRepository.findByIdAndOwner(any(), eq(currentUser)))
                        .thenReturn(Optional.empty());

                mockMvc.perform(get("/projects/{projectId}/versions/{versionId}", projectId, UUID.randomUUID()))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /projects/{projectId}/versions returns 200 with created version")
        void testCreateVersionReturns200() throws Exception {
                UUID versionId = UUID.randomUUID();
                VersionResponse created = buildVersionResponse(versionId, 2, "Sprint 2");

                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));
                when(versionService.createVersion(eq(project), any(VersionRequest.class)))
                        .thenReturn(created);

                VersionRequest request = new VersionRequest();
                request.setTitle("Sprint 2");

                mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.version").value(2))
                        .andExpect(jsonPath("$.title").value("Sprint 2"));
        }

        @Test
        @DisplayName("POST /projects/{projectId}/versions returns 404 when project not found")
        void testCreateVersionReturns404WhenProjectNotFound() throws Exception {
                when(projectRepository.findByIdAndOwner(any(), eq(currentUser)))
                        .thenReturn(Optional.empty());

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
                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));

                mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\": \"\"}"))
                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /projects/{projectId}/versions returns 400 when title exceeds 50 characters")
        void testCreateVersionReturns400WhenTitleTooLong() throws Exception {
                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));

                String longTitle = "A".repeat(51);

                mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\": \"" + longTitle + "\"}"))
                        .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /projects/{projectId}/versions returns 400 when body is missing")
        void testCreateVersionReturns400WhenBodyMissing() throws Exception {
                when(projectRepository.findByIdAndOwner(eq(projectId), eq(currentUser)))
                        .thenReturn(Optional.of(project));

                mockMvc.perform(post("/projects/{projectId}/versions", projectId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());
        }
}