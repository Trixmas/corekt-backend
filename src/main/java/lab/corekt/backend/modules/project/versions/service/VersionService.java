package lab.corekt.backend.modules.project.versions.service;

import jakarta.transaction.Transactional;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionIssuesResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class VersionService {
    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;

    public VersionService(VersionRepository versionRepository, ProjectRepository projectRepository) {
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public List<VersionResponse> getAllByProject(ProjectEntity project, UserEntity user) {
        projectRepository.findByIdAndOwner(project.getId(), user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versions not found"));

        return versionRepository.findFirstVersionByProject(project)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VersionResponse getOneVersion(ProjectEntity project, UUID versionId) {
        VersionEntity entity = getVersionEntity(project, versionId);
        return toResponse(entity);
    }

    @Transactional
    public VersionResponse createVersion(ProjectEntity project, VersionRequest request) {
        projectRepository.findById(project.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        // FIXME
        int nextVersionNumber = versionRepository.findLatestVersionByProject(project)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        VersionEntity entity = new VersionEntity(project, request.getTitle());
        entity.setVersionNumber(nextVersionNumber);

        VersionEntity saved = versionRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public VersionResponse updateVersion(ProjectEntity project, UUID versionId, VersionRequest request) {
        VersionEntity entity = getVersionEntity(project, versionId);

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }

        VersionEntity saved = versionRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteVersion(ProjectEntity project, UUID versionId) {
        VersionEntity entity = getVersionEntity(project, versionId);
        versionRepository.delete(entity);
    }

    private void ensureProjectExists(ProjectEntity project) {
        if (!projectRepository.existsById(project.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found");
        }
    }

    private VersionEntity getVersionEntity(ProjectEntity project, UUID versionId) {
        return versionRepository.findVersionByProjectAndVersionId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }

    private VersionResponse toResponse(VersionEntity entity) {
        VersionIssuesResponse issues = new VersionIssuesResponse(0, 0, 0, 0);

        VersionResponse response = new VersionResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setVersion(entity.getVersionNumber());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setIssues(issues);

        return response;
    }
}
