package lab.corekt.backend.modules.project.versions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateVersionRequest {

    @NotBlank
    @Size(max = 50)
    private String title;
}
