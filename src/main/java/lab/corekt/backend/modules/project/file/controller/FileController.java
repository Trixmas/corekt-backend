package lab.corekt.backend.modules.project.file.controller;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lab.corekt.backend.modules.enums.FileType;
import lab.corekt.backend.modules.project.file.dto.FileUploadResponse;
import lab.corekt.backend.modules.project.file.entity.FileEntity;
import lab.corekt.backend.modules.project.file.service.FileService;

@RestController
@RequestMapping("/projects/{projectId}/versions/{versionId}/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<FileUploadResponse> uploadFile(
        @PathVariable UUID projectId,
        @PathVariable Integer versionId,
        @RequestParam MultipartFile file,
        @RequestParam FileType fileType) throws IOException {

        FileEntity saved = fileService.uploadFile(projectId, versionId, file, fileType);
        FileUploadResponse response = new FileUploadResponse();

        response.setFileId(saved.getId());
        response.setFileName(saved.getName());
        response.setFileType(saved.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
