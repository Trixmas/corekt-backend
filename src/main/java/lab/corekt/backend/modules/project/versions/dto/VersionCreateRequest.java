package lab.corekt.backend.modules.project.versions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VersionCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Project id is required")
    private UUID projectId;
}
