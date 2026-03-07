package lab.corekt.backend.modules.project.versions.dto;

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
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public VersionResponse(Integer versionNumber, UUID relatedProjectId, String title, Integer score, String summary, String status) {
        this.versionNumber = versionNumber;
        this.relatedProjectId = relatedProjectId;
        this.title = title;
        this.score = score;
        this.summary = summary;
        this.status = status;

    }

    public VersionResponse() {
    }
}
