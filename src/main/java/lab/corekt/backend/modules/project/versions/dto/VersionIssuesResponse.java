package lab.corekt.backend.modules.project.versions.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VersionIssuesResponse {
    private Integer minor;
    private Integer major;
    private Integer critical;
    private Integer resolved;
}
