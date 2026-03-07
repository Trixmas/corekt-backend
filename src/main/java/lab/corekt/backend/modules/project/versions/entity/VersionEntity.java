package lab.corekt.backend.modules.project.versions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lab.corekt.backend.modules.project.entity.ProjectEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.Status;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
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
    private Integer id = 1;

    @Id
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false, length = 50)
    private String title;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files;

    @CreatedDate
    @Column(nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    // Constructeurs
    public VersionEntity() {
        this.status = Status.DRAFT;
    }

    public VersionEntity(String title) {
        this.title = title;
        this.status = Status.DRAFT;
    }

    //    "Trigger" to give versionId
    @PrePersist
    public void setVersionId() {
        List<VersionEntity> versions = this.project.getVersions();
        this.id = versions.size();
    }

    public FileEntity findFileByType(FileType fileType) {
        return this.files.stream().filter(file -> file.getType().equals(fileType)).findFirst().orElse(null);
    }

    public FileEntity findOrCreateFile(FileType fileType) {
        FileEntity file = this.findFileByType(fileType);
        if (file == null) {
            file = new FileEntity();
            file.setType(fileType);
            this.addFile(file);
        }
        return file;
    }

    public void addFile(FileEntity file) {
        this.files.add(file);
    }
}
