package lab.corekt.backend.modules.project.file.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lab.corekt.backend.modules.enums.FileType;
import lab.corekt.backend.modules.project.file.entity.FileEntity;
import lab.corekt.backend.modules.project.file.repository.FileRepository;
import lab.corekt.backend.modules.project.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.version.entity.VersionEntity;
import lab.corekt.backend.modules.project.version.entity.VersionId;
import lab.corekt.backend.modules.project.version.repository.VersionRepository;

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
