package lab.corekt.backend.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ProjectDetails {

    public ProjectDetails(ProjectEntity entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.description = entity.getDescription();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.totalVersion = (entity.getVersions() != null) ? entity.getVersions().size() : 1;
    }

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
