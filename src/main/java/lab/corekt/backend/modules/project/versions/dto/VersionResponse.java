package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
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

    public VersionResponse() {
    }

    public VersionResponse(VersionEntity version) {
        this.id = version.getId();
        this.title = version.getTitle();
        this.version = version.getVersionNumber();
        this.status = version.getStatus();
        this.createdAt = version.getCreatedAt();
        this.updatedAt = version.getUpdatedAt();
        this.setIssues(new VersionIssuesResponse(0, 0, 0, 0));
    }
}