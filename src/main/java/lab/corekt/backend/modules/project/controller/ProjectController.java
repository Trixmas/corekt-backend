package lab.corekt.backend.modules.project.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.ProjectDetails;
import lab.corekt.backend.modules.project.dto.UpdateProjectRequest;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.service.ProjectService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/projects")
class ProjectController {

    private final ProjectService projectService;

    private final AuthContext authContext;

    public ProjectController(ProjectService projectService, AuthContext authContext) {
        this.projectService = projectService;
        this.authContext = authContext;
    }


    @GetMapping
    public ResponseEntity<List<ProjectDetails>> getAllOwnersProjects() {
        UserEntity user = authContext.getCurrentUser();
        List<ProjectEntity> projects = projectService.getAllOwnersProjects(user);

        List<ProjectDetails> projectDetailsList = projects.stream().map(ProjectDetails::new).toList();

        return ResponseEntity.ok(projectDetailsList);
    }

    @PostMapping
    public ResponseEntity<ProjectDetails> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UserEntity user = authContext.getCurrentUser();

        ProjectEntity project = projectService.createProject(request, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectDetails(project));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetails> getProjectById(@Valid @PathVariable UUID projectId) {
        UserEntity currentUser = authContext.getCurrentUser();

        ProjectEntity project = projectService.getOwnersProjectById(projectId, currentUser);

        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(new ProjectDetails(project));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDetails> updateProject(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest request) {
        UserEntity currentUser = authContext.getCurrentUser();
        ProjectEntity updatedProject = projectService.updateProject(projectId, request, currentUser);

        return ResponseEntity.ok(new ProjectDetails(updatedProject));
    }
}
