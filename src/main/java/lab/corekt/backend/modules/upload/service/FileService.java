package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.service.BpmnParserService;
import lab.corekt.backend.modules.project.parsing.service.McdParserService;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
@AllArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final BpmnParserService bpmnParserService;
    private final McdParserService mcdParserService;
    private final MistralService mistralService;

    @Transactional
    public FileEntity uploadFile(VersionEntity version, @NonNull MultipartFile file, @NonNull FileType fileType)
        throws IOException, DataIntegrityViolationException {

        if (!fileType.isValidFile(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type or extension for " + fileType);
        }
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }

        try {
            FileEntity fileEntity = new FileEntity(fileType, file.getOriginalFilename());
            fileEntity.setVersion(version);
            fileEntity.setFile(file.getBytes());

            processFileByType(fileType, fileEntity);
            return fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new IOException("Failed to read file content", e);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Failed to save file, a file of type " + fileType + " already exists for this version.", e);
        }
    }

    private void processFileByType(@NonNull FileType fileType, FileEntity fileEntity) {
        try {
            switch (fileType) {
                case BPMN -> bpmnParserService.processFile(fileEntity);
                case MCD -> mcdParserService.processFile(fileEntity);
                case INTERVIEW -> mistralService.processInterview(fileEntity);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse the uploaded file: " + e.getMessage());
        }
    }

    /**
     * Returns the existing {@link FileEntity} of the given type for the given
     * version,
     * or creates and persists a new one if none exists.
     *
     * @param fileType the type of file (e.g. US, ISSUES…)
     * @param fileName human-readable display name for the file
     * @param version  the version this file belongs to
     * @return the existing or newly created {@link FileEntity}
     */
    @Transactional
    public FileEntity getOrCreate(FileType fileType, String fileName, VersionEntity version) {
        return fileRepository.findByTypeAndVersion(fileType, version)
            .orElseGet(() -> {
                FileEntity newFile = new FileEntity(fileType, fileName);
                newFile.setVersion(version);
                return fileRepository.save(newFile);
            });
    }
}
