package lab.corekt.backend.modules.upload.controller;

import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = FileController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class FileControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private VersionRepository versionRepository;

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

        project = new ProjectEntity("Projet test", "Description test", user);
        project.setId(projectId);

        version = new VersionEntity(project, "Version 1");
        version.setId(versionId);
        version.setVersionNumber(1);
    }

    @Test
    @DisplayName("upload BPMN returns 201")
    void testUploadBPMNReturns201() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes());

        FileEntity saved = new FileEntity(FileType.BPMN, "diagram.bpmn");
        saved.setId(UUID.randomUUID());
        saved.setVersion(version);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(fileService.uploadFile(version, multipartFile, FileType.BPMN)).thenReturn(saved);

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                    .file(multipartFile)
                    .param("fileType", "BPMN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("diagram.bpmn"))
                .andExpect(jsonPath("$.fileType").value("BPMN"));
    }

    @Test
    @DisplayName("upload MCD returns 201")
    void testUploadMCDReturns201() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "schema.mcd", "text/plain", "MCD".getBytes());

        FileEntity saved = new FileEntity(FileType.MCD, "schema.mcd");
        saved.setId(UUID.randomUUID());
        saved.setVersion(version);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(fileService.uploadFile(version, multipartFile, FileType.MCD)).thenReturn(saved);

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                    .file(multipartFile)
                    .param("fileType", "MCD"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("schema.mcd"))
                .andExpect(jsonPath("$.fileType").value("MCD"));
    }

    @Test
    @DisplayName("upload returns 404 when project is missing")
    void testUploadReturns404WhenProjectIsMissing() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes());

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                    .file(multipartFile)
                    .param("fileType", "BPMN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("upload returns 404 when version is missing")
    void testUploadReturns404WhenVersionIsMissing() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile("file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                    .file(multipartFile)
                    .param("fileType", "BPMN"))
                .andExpect(status().isNotFound());
    }
}