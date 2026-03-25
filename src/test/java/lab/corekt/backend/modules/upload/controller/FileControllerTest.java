package lab.corekt.backend.modules.upload.controller;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
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

    @MockitoBean
    private AuthContext authContext;

    @MockitoBean
    private BpmnParserService bpmnParserService;

    @MockitoBean
    private McdParserService mcdParserService;

    private UUID projectId;
    private UUID versionId;
    private UserEntity user;
    private ProjectEntity project;
    private VersionEntity version;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        user = new UserEntity("alice@example.com", "alice");
        user.setId(UUID.randomUUID());

        project = new ProjectEntity("Projet test", "Description test", user);
        project.setId(projectId);

        version = new VersionEntity(project, "Version 1");
        version.setId(versionId);
        version.setVersionNumber(1);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /projects/{projectId}/versions/{versionId}/files returns 201 with file payload")
    void uploadReturns201WithSavedFile() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        FileEntity saved = new FileEntity(FileType.BPMN, "diagram.bpmn");
        saved.setId(UUID.randomUUID());
        saved.setVersion(version);

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(fileService.uploadFile(version, multipartFile, FileType.BPMN)).thenReturn(saved);

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                .file(multipartFile)
                .param("fileType", "BPMN"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileId").value(saved.getId().toString()))
            .andExpect(jsonPath("$.fileName").value("diagram.bpmn"))
            .andExpect(jsonPath("$.fileType").value("BPMN"));

        verify(fileService).uploadFile(version, multipartFile, FileType.BPMN);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /projects/{projectId}/versions/{versionId}/files returns 404 when project is missing")
    void uploadReturns404WhenProjectIsMissing() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                .file(multipartFile)
                .param("fileType", "BPMN"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Project not found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /projects/{projectId}/versions/{versionId}/files returns 404 when version is missing")
    void uploadReturns404WhenVersionIsMissing() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                .file(multipartFile)
                .param("fileType", "BPMN"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Version not found"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /projects/{projectId}/versions/{versionId}/files returns 409 on duplicate file type")
    void uploadReturns409WhenFileTypeAlreadyExists() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
            "file", "diagram.bpmn", "application/xml", "<bpmn />".getBytes()
        );

        when(authContext.getCurrentUser()).thenReturn(user);
        when(projectRepository.findByIdAndOwner(projectId, user)).thenReturn(Optional.of(project));
        when(versionRepository.findByProjectAndId(project, versionId)).thenReturn(Optional.of(version));
        when(fileService.uploadFile(version, multipartFile, FileType.BPMN))
            .thenThrow(new DataIntegrityViolationException(
                "Failed to save file, a file of type BPMN already exists for this version."
            ));

        mockMvc.perform(multipart("/projects/{projectId}/versions/{versionId}/files", projectId, versionId)
                .file(multipartFile)
                .param("fileType", "BPMN"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "Data integrity violation: Failed to save file, a file of type BPMN already exists for this version."
            ));
    }
}
