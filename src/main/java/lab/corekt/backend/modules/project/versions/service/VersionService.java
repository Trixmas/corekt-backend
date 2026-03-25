package lab.corekt.backend.modules.project.versions.service;

import jakarta.transaction.Transactional;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lombok.AllArgsConstructor;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VersionService {
    private final VersionRepository versionRepository;
    private final DataLinkRepository dataLinkRepository;

    @Transactional
    public List<VersionResponse> getAllByProject(ProjectEntity project) {
        return versionRepository.findByProject(project)
            .stream()
            .map(VersionResponse::new)
            .toList();
    }

    @Transactional
    public VersionDetailsResponse getVersion(ProjectEntity project, UUID versionId) {
        VersionEntity version = getVersionEntity(project, versionId);
        return new VersionDetailsResponse(version, getDataLinks(version));
    }

    @Transactional
    public VersionResponse createVersion(ProjectEntity project, VersionRequest request) {
        int nextVersionNumber = versionRepository
            .findTopByProjectOrderByVersionNumberDesc(project)
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);

        VersionEntity entity = new VersionEntity(project, request.getTitle());
        entity.setVersionNumber(nextVersionNumber);

        return new VersionResponse(versionRepository.save(entity));
    }

    @Transactional
    public VersionResponse updateVersion(ProjectEntity project, UUID versionId, VersionRequest request) {
        VersionEntity entity = getVersionEntity(project, versionId);

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }

        return new VersionResponse(versionRepository.save(entity));
    }

    @Transactional
    public void deleteVersion(ProjectEntity project, UUID versionId) {
        VersionEntity entity = getVersionEntity(project, versionId);
        versionRepository.delete(entity);
    }

    public void setVersionStatus(VersionEntity version, VersionStatus status) {
        version.setStatus(status);
        versionRepository.save(version);
    }

    public int countByProjectOwnerAndStatus(UserEntity user, VersionStatus status) {
        return (int) versionRepository.countByProjectOwnerAndStatus(user, status);
    }

    private VersionEntity getVersionEntity(ProjectEntity project, UUID versionId) {
        return versionRepository
            .findByProjectAndId(project, versionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }


    private List<DataLinkEntity> getDataLinks(VersionEntity version) {
        return dataLinkRepository.findByVersionId(version.getId());
    }
}
