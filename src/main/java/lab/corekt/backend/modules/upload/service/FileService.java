package lab.corekt.backend.modules.upload.service;

import java.io.IOException;

import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

@Service
public class FileService {
    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Transactional
    public FileEntity uploadFile(VersionEntity version, MultipartFile file, FileType fileType) throws IOException {
        boolean alreadyExists = false;
        for (FileEntity existingFile : version.getFiles()) {
            if (existingFile.getType() == fileType) {
                alreadyExists = true;
                break;
            }
        }

        if (alreadyExists) {
            throw new IllegalArgumentException("A file of type " + fileType + " already exists for this version");
        }

        FileEntity fileEntity = new FileEntity(fileType, file.getOriginalFilename(), version);
        fileEntity.setFile(file.getBytes());

        return fileRepository.save(fileEntity);
    }
}
