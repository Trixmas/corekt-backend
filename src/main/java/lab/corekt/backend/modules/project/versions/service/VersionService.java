package lab.corekt.backend.modules.project.versions.service;

import jakarta.transaction.Transactional;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.versions.dto.VersionIssuesResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class VersionService {
    private final VersionRepository versionRepository;

    public VersionService(VersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional
    public List<VersionResponse> getAllByProject(ProjectEntity project) {
        return versionRepository.findByProject(project)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VersionResponse getOneVersion(ProjectEntity project, UUID versionId) {
        return toResponse(getVersionEntity(project, versionId));
    }

    @Transactional
    public VersionResponse createVersion(ProjectEntity project, VersionRequest request) {
        int nextVersionNumber = versionRepository.findTopByProjectOrderByVersionNumberDesc(project)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        VersionEntity entity = new VersionEntity(project, request.getTitle());
        entity.setVersionNumber(nextVersionNumber);

        return toResponse(versionRepository.save(entity));
    }

    @Transactional
    public VersionResponse updateVersion(ProjectEntity project, UUID versionId, VersionRequest request) {
        VersionEntity entity = getVersionEntity(project, versionId);

        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }

        return toResponse(versionRepository.save(entity));
    }

    @Transactional
    public void deleteVersion(ProjectEntity project, UUID versionId) {
        VersionEntity entity = getVersionEntity(project, versionId);
        versionRepository.delete(entity);
    }

    private VersionEntity getVersionEntity(ProjectEntity project, UUID versionId) {
        return versionRepository.findByProjectAndId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }

    private VersionResponse toResponse(VersionEntity entity) {
        VersionResponse response = new VersionResponse();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setVersion(entity.getVersionNumber());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setIssues(new VersionIssuesResponse(0, 0, 0, 0));
        return response;
    }
}