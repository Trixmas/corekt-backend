package lab.corekt.backend.modules.upload.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;

@Service
public class FileService {

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
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
