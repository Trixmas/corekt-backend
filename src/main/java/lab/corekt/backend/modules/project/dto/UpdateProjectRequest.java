package lab.corekt.backend.modules.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

    @Size(max = 50)
    private String title;

    private String description;
}
