package lab.corekt.backend.modules.project.versions.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.VersionStatus;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "version_number"})
})
@EntityListeners(AuditingEntityListener.class)
public class VersionEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "version_id")
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Integer versionNumber;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private ProjectEntity project;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 50)
    private String title;

    @Column
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    public VersionEntity() {
        this.status = VersionStatus.DRAFT;
        this.versionNumber = 1;
        this.summary = "";
    }

    public VersionEntity(String title) {
        this();
        this.title = title;
    }

    public VersionEntity(ProjectEntity project, String title) {
        this(title);
        this.project = project;
    }
}
