package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
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

    public VersionResponse(VersionEntity version, List<DataLinkEntity> dataLinks) {
        this.id = version.getId();
        this.title = version.getTitle();
        this.version = version.getVersionNumber();
        this.status = version.getStatus();
        this.createdAt = version.getCreatedAt();
        this.updatedAt = version.getUpdatedAt();

        Number critical = dataLinks.stream().filter(dl -> dl.getSeverity() == Severity.CRITICAL).count();
        Number major = dataLinks.stream().filter(dl -> dl.getSeverity() == Severity.MAJOR).count();
        Number minor = dataLinks.stream().filter(dl -> dl.getSeverity() == Severity.MINOR).count();
        this.setIssues(new VersionIssuesResponse(minor.intValue(), major.intValue(), critical.intValue()));
    }
}