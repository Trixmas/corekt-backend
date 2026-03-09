package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class VersionResponse {
    private UUID id;
    private String title;
    private Integer version;
    private VersionStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private VersionIssuesResponse issues;
}