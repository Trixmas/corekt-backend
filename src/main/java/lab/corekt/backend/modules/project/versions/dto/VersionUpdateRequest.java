package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionUpdateRequest {
    private String title;
    private Integer score;
    private String summary;
    private Status status;
}
