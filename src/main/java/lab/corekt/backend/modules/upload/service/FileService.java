package lab.corekt.backend.modules.upload.service;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.event.FileUploadedEvent;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.enums.ProcessingStatus;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
            fileEntity.setProcessingStatus(ProcessingStatus.PENDING);
            FileEntity savedFile = fileRepository.save(fileEntity);

            // Processing runs in a dedicated async listener after this transaction commits.
            eventPublisher.publishEvent(new FileUploadedEvent(savedFile.getId()));

            return savedFile;
        } catch (IOException e) {
            throw new IOException("Failed to read file content", e);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Failed to save file, a file of type " + fileType + " already exists for this version.", e);
        }
    }


    @Transactional
    public void deleteFile(FileEntity fileEntity) {
        fileRepository.delete(fileEntity);
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
