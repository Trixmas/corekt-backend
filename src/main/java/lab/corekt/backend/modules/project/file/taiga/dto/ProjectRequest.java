package lab.corekt.backend.modules.project.file.taiga.dto;

import jakarta.validation.constraints.NotBlank;

public class ProjectRequest {
    @NotBlank(message = "Project id is required")
    private String project_id;
}
