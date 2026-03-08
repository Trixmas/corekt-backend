package lab.corekt.backend.modules.project.versions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 50)
    private String title;
}
