package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lab.corekt.backend.modules.project.enums.Status;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "version")
@EntityListeners(AuditingEntityListener.class)
@IdClass(VersionId.class)
public class VersionEntity {

    @Id
    @Column(name = "version_id")
    @Min(1)
    @Max(10)
    private Integer versionId = 1;

    @Id
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 50)
    private String versionTitle;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files;

    // Constructeurs
    public VersionEntity() {
        this.status = Status.DRAFT;
    }

    public VersionEntity(ProjectEntity project, String versionTitle) {
        this.project = project;
        this.versionTitle = versionTitle;
        this.status = Status.DRAFT;
    }

    //    "Trigger" to give versionId
    @PrePersist
    public void giveVersionId() {
        List<VersionEntity> versions = this.project.getVersions();
        this.versionId = versions.size() + 1;
    }
}
