package lab.corekt.backend.modules.project.versions.service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.springframework.stereotype.Service;

@Service
public class VersionService {

    private final VersionRepository versionRepository;

    VersionService(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    public VersionEntity createVersion(ProjectEntity project, String title) {
        VersionEntity version = new VersionEntity(title);
        version.setProject(project);
        return versionRepository.save(version);
    }

    public void updateVersion(VersionEntity version, String title) {
        version.setTitle(title);
        versionRepository.save(version);
    }
}
