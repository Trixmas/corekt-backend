package lab.corekt.backend.modules.upload.controller;

import java.io.IOException;
import java.util.UUID;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionId;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import lab.corekt.backend.modules.upload.dto.FileUploadResponse;
import lab.corekt.backend.modules.upload.service.FileService;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/files")
public class FileController {

    private final FileService fileService;
    private final ProjectRepository projectRepository;
    private final VersionRepository versionRepository;

    public FileController(FileService fileService, ProjectRepository projectRepository, VersionRepository versionRepository) {
        this.fileService = fileService;
        this.projectRepository = projectRepository;
        this.versionRepository = versionRepository;
    }

    @PostMapping
    public ResponseEntity<FileUploadResponse> uploadFile(
        @PathVariable UUID projectId,
        @PathVariable Integer versionId,
        @RequestParam MultipartFile file,
        @RequestParam FileType fileType) throws IOException {

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        VersionEntity version = versionRepository.findById(new VersionId(versionId, project))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        FileEntity saved = fileService.uploadFile(version, file, fileType);
        FileUploadResponse response = new FileUploadResponse();

        response.setFileId(saved.getId());
        response.setFileName(saved.getName());
        response.setFileType(saved.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
