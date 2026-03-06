package lab.corekt.backend.modules.project.version.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VersionRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Project id is required")
    private UUID projectId;
}
