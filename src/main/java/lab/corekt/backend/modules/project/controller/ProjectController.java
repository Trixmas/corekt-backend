package lab.corekt.backend.modules.project.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.ProjectResponse;
import lab.corekt.backend.modules.project.dto.UpdateProjectRequest;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.service.ProjectService;
import lab.corekt.backend.modules.project.utils.ProjectUtils;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/projects")
class ProjectController {

    private final ProjectService projectService;
    private final AuthContext authContext;
    private final ProjectUtils projectUtils;

    public ProjectController(ProjectService projectService, AuthContext authContext, ProjectUtils projectUtils) {
        this.projectService = projectService;
        this.authContext = authContext;
        this.projectUtils = projectUtils;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllOwnersProjects() {
        UserEntity user = authContext.getCurrentUser();
        List<ProjectEntity> projects = projectService.getAllOwnerProjects(user);
        List<ProjectResponse> projectDetailsList = projects.stream().map(ProjectResponse::new).toList();
        return ResponseEntity.ok(projectDetailsList);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UserEntity user = authContext.getCurrentUser();
        ProjectEntity project = projectService.createProject(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectResponse(project));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@Valid @PathVariable UUID projectId) {
        ProjectEntity project = projectUtils.resolveProject(projectId);

        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new ProjectResponse(project));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request) {
        UserEntity currentUser = authContext.getCurrentUser();
        ProjectEntity updatedProject = projectService.updateProject(projectId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectResponse(updatedProject));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId, authContext.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
