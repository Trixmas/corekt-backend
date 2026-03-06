package lab.corekt.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ProjectDetails {

    @NotBlank(message = "Id is required")
    private UUID id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private int totalVersion;

    private Instant createdAt;

    private Instant updatedAt;

}
