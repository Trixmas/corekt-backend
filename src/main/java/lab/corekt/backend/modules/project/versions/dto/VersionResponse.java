package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.enums.Status;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public class VersionResponse {
    private Integer versionNumber;
    private UUID relatedProjectId;
    private String title;
    private Integer score;
    private String summary;
    private Status status;
    private Instant createdAt;
    private Instant updatedAt;

    public VersionResponse(VersionEntity version) {
        this.versionNumber = version.getId();
        this.relatedProjectId = version.getProject().getId();
        this.title = version.getTitle();
        this.score = version.getScore();
        this.summary = version.getSummary();
        this.status = version.getStatus();
        this.createdAt = version.getCreatedAt();
        this.updatedAt = version.getUpdatedAt();
    }

    public VersionResponse() {
    }
}
