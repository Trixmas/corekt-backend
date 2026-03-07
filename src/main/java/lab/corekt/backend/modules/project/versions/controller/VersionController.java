package lab.corekt.backend.modules.project.versions.controller;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/project/{projectId}/versions")
class VersionController {

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final AuthContext authContext;

    VersionController(VersionRepository versionRepository, ProjectRepository projectRepository, AuthContext authContext) {
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.authContext = authContext;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getProjectVersions(@PathVariable UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        ProjectEntity project = projectRepository.findByIdAndOwner(projectId, user).orElse(null);
        if (project == null) {
            return ResponseEntity.status(404).body(null);
        }
        List<VersionEntity> versions = versionRepository.findAllByProject(project);

        List<VersionResponse> versionResponses = versions.stream()
                .map(version ->
                {
                    VersionResponse response = new VersionResponse();
                    response.setVersionNumber(version.getId());
                    response.setRelatedProjectId(version.getProject().getId());
                    response.setTitle(version.getTitle());
                    response.setScore(version.getScore());
                    response.setSummary(version.getSummary());
                    response.setScore(version.getScore());
                    response.setCreatedAt(version.getCreatedAt());
                    response.setUpdatedAt(version.getUpdatedAt());

                    return response;
                }).toList();

        return ResponseEntity.ok(versionResponses);
    }
}
