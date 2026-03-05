package lab.corekt.backend.modules.upload.service;

import java.io.IOException;
import java.util.UUID;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionId;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;

@Service
public class FileService {
    private final FileRepository fileRepository;
    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;

    public FileService(FileRepository fileRepository, VersionRepository versionRepository, ProjectRepository projectRepository) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public FileEntity uploadFile(UUID projectId, Integer versionId, MultipartFile file, FileType fileType)
            throws IOException {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        VersionId id = new VersionId(versionId, project);
        VersionEntity version = versionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

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
