package lab.corekt.backend.modules.upload.controller;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.dto.FileUploadResponse;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.service.FileService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/files")
public class FileController {

    private final FileService fileService;
    private final ProjectRepository projectRepository;
    private final VersionRepository versionRepository;
    private final AuthContext authContext;

    public FileController(FileService fileService, ProjectRepository projectRepository, VersionRepository versionRepository, AuthContext authContext) {
        this.fileService = fileService;
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
        this.authContext = authContext;
    }

    @PostMapping
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable UUID projectId,
            @PathVariable UUID versionId,
            @RequestParam MultipartFile file,
            @RequestParam FileType fileType) throws IOException {

        UserEntity user = authContext.getCurrentUser();
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        VersionEntity version = versionRepository.findByProjectAndId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        FileEntity saved = fileService.uploadFile(version, file, fileType);

        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(saved.getId());
        response.setFileName(saved.getName());
        response.setFileType(saved.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}