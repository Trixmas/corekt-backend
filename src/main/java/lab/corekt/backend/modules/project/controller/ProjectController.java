package lab.corekt.backend.modules.project.controller;

import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.project.dto.ProjectDetails;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.service.ProjectService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/projects")
class ProjectController {

    private final ProjectService projectService;

    private final UserService userService;
    private final AuthContext authContext;

    public ProjectController(ProjectService projectService, UserService userService, AuthContext authContext) {
        this.projectService = projectService;
        this.userService = userService;
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
    public ResponseEntity<ProjectDetails> createProject(@RequestBody Map<String, String> payload) {
        UserEntity user = authContext.getCurrentUser();
        String title = payload.get("title");
        String description = payload.get("description");

        if (title == null || title.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ProjectEntity project = projectService.createProject(title, user);
        project.setDescription(description);

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
