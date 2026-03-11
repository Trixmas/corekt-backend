package lab.corekt.backend.modules.project.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.ProjectResponse;
import lab.corekt.backend.modules.project.dto.UpdateProjectRequest;
import lab.corekt.backend.modules.project.service.ProjectService;
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

    public ProjectController(ProjectService projectService, AuthContext authContext) {
        this.projectService = projectService;
        this.authContext = authContext;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllOwnersProjects() {
        UserEntity user = authContext.getCurrentUser();
        return ResponseEntity.ok(projectService.getAllOwnerProjects(user));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UserEntity user = authContext.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request, user));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable UUID projectId) {
        UserEntity user = authContext.getCurrentUser();
        ProjectResponse response = projectService.getOwnersProjectById(projectId, user);

        if (response == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request) {
        UserEntity currentUser = authContext.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.updateProject(projectId, request, currentUser));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId, authContext.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
