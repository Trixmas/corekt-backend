package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Service for managing analysis operations on project versions.
 * Handles data link deletion with proper validation and error handling.
 */
@Service
@AllArgsConstructor
public class AnalysisService {

    private final DataLinkRepository dataLinkRepository;
    private final ProjectUtils projectUtils;
    private final VersionRepository versionRepository;

    /**
     * Deletes a data link from a project version.
     *
     * @param projectId the UUID of the project
     * @param versionId the UUID of the version
     * @param dataLinkId the UUID of the data link to delete
     * @throws ResponseStatusException if project, version, or data link not found
     */
    public void deleteDataLink(UUID projectId, UUID versionId, UUID dataLinkId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        VersionEntity version = versionRepository
                .findByProjectAndId(project, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        DataLinkEntity toDelete = dataLinkRepository.findByIdAndVersionId(dataLinkId, version.getId());
        if (toDelete == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Data link not found");
        }

        dataLinkRepository.delete(toDelete);
    }
}
