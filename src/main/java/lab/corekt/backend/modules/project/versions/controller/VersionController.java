package lab.corekt.backend.modules.project.versions.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.repository.ProjectRepository;
import lab.corekt.backend.modules.project.versions.dto.VersionResponse;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.project.versions.repository.VersionRepository;
import lab.corekt.backend.modules.user.entity.UserEntity;

@RestController
@RequestMapping("/project/{projectId}/versions")
class VersionController {

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final AuthContext authContext;

    VersionController(VersionRepository versionRepository, ProjectRepository projectRepository,
            AuthContext authContext) {
        this.versionRepository = versionRepository;
        this.projectRepository = projectRepository;
        this.authContext = authContext;
    }

    @GetMapping
    public ResponseEntity<List<VersionResponse>> getProjectVersions(@PathVariable UUID projectId) {
        ProjectEntity project = resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        List<VersionResponse> versionResponses = versionRepository.findAllByProject(project)
                .stream()
                .map(this::toVersionResponse)
                .toList();

        return ResponseEntity.ok(versionResponses);
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<VersionResponse> getProjectVersion(
            @PathVariable UUID projectId,
            @PathVariable Integer versionId) {
        ProjectEntity project = resolveProject(projectId);
        if (project == null)
            return ResponseEntity.notFound().build();

        return versionRepository.findByIdAndProject(versionId, project)
                .map(this::toVersionResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ProjectEntity resolveProject(UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        return projectRepository.findByIdAndOwner(projectId, user).orElse(null);
    }

    private VersionResponse toVersionResponse(VersionEntity version) {
        VersionResponse response = new VersionResponse();
        response.setVersionNumber(version.getId());
        response.setRelatedProjectId(version.getProject().getId());
        response.setTitle(version.getTitle());
        response.setScore(version.getScore());
        response.setSummary(version.getSummary());
        response.setCreatedAt(version.getCreatedAt());
        response.setUpdatedAt(version.getUpdatedAt());
        return response;
    }
}
