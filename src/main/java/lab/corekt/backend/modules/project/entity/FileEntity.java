package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.enums.FileType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueFileTypeForVersion", columnNames = {"version_id", "project_id", "fileType"})
})
@EntityListeners(AuditingEntityListener.class)
public class FileEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "file_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType type;

    @Column(name = "file_name", nullable = false, length = 50)
    private String name;

    @Column(columnDefinition = "bytea")
    private byte[] file;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "version_id", referencedColumnName = "version_id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false, insertable = false, updatable = false)
    })
    private VersionEntity version;

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private AIAnalysisEntity aiAnalysis;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParsedDataEntity> parsedData;

    // Constructeurs
    public FileEntity() {
    }

    public FileEntity(FileType fileType, String fileName, Integer versionId, UUID projectId) {
        this.type = fileType;
        this.name = fileName;
        this.versionId = versionId;
        this.projectId = projectId;
    }
}

