package lab.corekt.backend.modules.project.versions.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.Status;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
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
    private UUID id;

    @Column(nullable = false)
    private Integer versionNumber;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 50, columnDefinition = "TEXT")
    private String summary;

    private String title;

    @Column
    private Integer score;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    protected VersionEntity() {
        this.status = Status.DRAFT;
    }

    public VersionEntity(ProjectEntity project, String title) {
        this.project = project;
        this.title = title;
        this.status = Status.DRAFT;
    }
}