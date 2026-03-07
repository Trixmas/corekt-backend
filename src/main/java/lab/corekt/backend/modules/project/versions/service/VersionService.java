package lab.corekt.backend.modules.project.versions.service;

import jakarta.transaction.Transactional;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionCreateRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionIssuesResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionUpdateRequest;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.MissingResourceException;
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
    public List<VersionResponse> getAllByProjectId(UUID projectId) {
        ensureProjectExists(projectId);

        return versionRepository.findFirstVersionByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VersionResponse getOneVersion(UUID projectId, UUID versionId) {
        VersionEntity entity = getVersionEntity(projectId, versionId);
        return toResponse(entity);
    }

    @Transactional
    public VersionResponse createVersion(UUID projectId, VersionCreateRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        int nextVersionNumber = versionRepository.findLatestVersionByProjectId(projectId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        VersionEntity entity = new VersionEntity(project, request.getTitle());
        entity.setVersionNumber(nextVersionNumber);

        VersionEntity saved = versionRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public VersionResponse updateVersion(UUID projectId, UUID versionId, VersionUpdateRequest request) {
        VersionEntity entity = getVersionEntity(projectId, versionId);

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }
        if (request.getScore() != null) {
            entity.setScore(request.getScore());
        }
        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        VersionEntity saved = versionRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteVersion(UUID projectId, UUID versionId) {
        VersionEntity entity = getVersionEntity(projectId, versionId);
        versionRepository.delete(entity);
    }

    private void ensureProjectExists(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found");
        }
    }

    private VersionEntity getVersionEntity(UUID projectId, UUID versionId) {
        return versionRepository.findVersionByProjectIdAndVersionId(projectId, versionId)
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
