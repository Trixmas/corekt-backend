package lab.corekt.backend.modules.project.controller;

import jakarta.validation.Valid;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.dto.CreateProjectRequest;
import lab.corekt.backend.modules.project.dto.ProjectDetails;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.service.ProjectService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        List<ProjectEntity> projects = projectService.findAllOwnersProjects(user);

        List<ProjectDetails> projectDetailsList = projects.stream().map(project -> {
            ProjectDetails projectDetails = new ProjectDetails();
            projectDetails.setId(project.getId());
            projectDetails.setTitle(project.getTitle());
            projectDetails.setDescription(project.getDescription());
            projectDetails.setTotalVersion(1);
            projectDetails.setCreatedAt(project.getCreatedAt());
            projectDetails.setUpdatedAt(project.getUpdatedAt());
            return projectDetails;
        }).toList();

        return ResponseEntity.ok(projectDetailsList);
    }

    @PostMapping
    public ResponseEntity<ProjectDetails> createProject(@Valid @RequestBody CreateProjectRequest request) {
        UserEntity user = authContext.getCurrentUser();
        String title = request.getTitle();
        String description = request.getDescription();

        ProjectEntity project = projectService.createProject(title, description, user);

        ProjectDetails projectDetails = new ProjectDetails();
        projectDetails.setId(project.getId());
        projectDetails.setTitle(project.getTitle());
        projectDetails.setDescription(project.getDescription());
        projectDetails.setTotalVersion(1);
        projectDetails.setCreatedAt(project.getCreatedAt());
        projectDetails.setUpdatedAt(project.getUpdatedAt());

        return ResponseEntity.ok(projectDetails);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetails> getProjectById(@Valid @PathVariable UUID projectId) {
        UserEntity currentUser = authContext.getCurrentUser();

        ProjectEntity project = projectService.findProjectById(projectId);

        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!project.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ProjectDetails projectDetails = new ProjectDetails();
        projectDetails.setId(project.getId());
        projectDetails.setTitle(project.getTitle());
        projectDetails.setDescription(project.getDescription());
        projectDetails.setTotalVersion(1);
        projectDetails.setCreatedAt(project.getCreatedAt());
        projectDetails.setUpdatedAt(project.getUpdatedAt());

        return ResponseEntity.ok(projectDetails);
    }
}
