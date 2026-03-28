package lab.corekt.backend.modules.project.versions.service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionDetailsResponse;
import lab.corekt.backend.modules.project.versions.dto.VersionRequest;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VersionService {

    private static final int MAX_NUMBER_VERSION = 10;
    private static final int PERFECT_ANALYSIS_RATE = 90;

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final DataLinkRepository dataLinkRepository;

    @Transactional
    public List<VersionResponse> getAllByProject(ProjectEntity project) {
        return versionRepository.findByProject(project)
                .stream()
                .map(version -> new VersionResponse(version, getDataLinks(version)))
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

        return new VersionResponse(versionRepository.save(entity), new ArrayList<>());
    }

    @Transactional
    public VersionResponse updateVersion(ProjectEntity project, UUID versionId, VersionRequest request) {
        VersionEntity entity = getVersionEntity(project, versionId);
        if (request.getTitle() != null) {
            entity.setTitle(request.getTitle());
        }

        return new VersionResponse(versionRepository.save(entity), getDataLinks(entity));
    }

    @Transactional
    public void deleteVersion(ProjectEntity project, UUID versionId) {
        VersionEntity entity = getVersionEntity(project, versionId);
        versionRepository.delete(entity);
    }

    @Transactional
    public void setVersionStatus(VersionEntity version, VersionStatus status) {
        version.setStatus(status);
        versionRepository.save(version);
    }

    @Transactional
    public int countByProjectOwnerAndStatus(UserEntity user, VersionStatus status) {
        return (int) versionRepository.countByProjectOwnerAndStatus(user, status);
    }


    public float getVersionScore(VersionEntity version) {
        List<DataLinkEntity> dataLinks = dataLinkRepository.findByVersionId(version.getId());
        float sumConfidenceScore = 0;
        for (DataLinkEntity dataLink : dataLinks) {
            sumConfidenceScore += dataLink.getConfidenceScore();
        }
        return sumConfidenceScore / dataLinks.size();
    }


    public VersionEntity getLatestVersion(ProjectEntity project) {
        return versionRepository.findTopByProjectOrderByVersionNumberDesc(project).orElse(null);
    }


    public float getProjectScore(ProjectEntity project) {
        return getVersionScore(getLatestVersion(project));
    }

    @Transactional(readOnly = true)
    public int countProjectsWithPerfectAnalysis(UserEntity user) {
        return (int) projectRepository.findByOwner(user)
                .stream()
                .filter(project -> getProjectScore(project) >= PERFECT_ANALYSIS_RATE)
                .count();
    }

    @Transactional(readOnly = true)
    public int countProjectsWithPerfectAnalysisAtFirst(UserEntity user) {
        return (int) projectRepository.findByOwner(user)
                .stream()
                .filter(project -> project.getVersions().size() == 1 && getProjectScore(project) >= PERFECT_ANALYSIS_RATE)
                .count();
    }

    @Transactional(readOnly = true)
    public int countProjectsWithPerfectAnalysisAtLastVersion(UserEntity user) {
        return (int) projectRepository.findByOwner(user)
                .stream()
                .filter(project -> project.getVersions().size() == MAX_NUMBER_VERSION && getProjectScore(project) >= PERFECT_ANALYSIS_RATE)
                .count();
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
