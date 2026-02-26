package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.enums.FileType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "file", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueFileTypeForVersion", columnNames = {"version_id", "project_id", "fileType"})
})
@EntityListeners(AuditingEntityListener.class)
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    @Column(nullable = false, length = 50)
    private String fileName;

    @Column(columnDefinition = "bytea")
    private byte[] file;

    @ManyToOne
    @JoinColumn(name = "version_id", referencedColumnName = "version_id", nullable = false, insertable = false, updatable = false)
    @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false, insertable = false, updatable = false)
    private VersionEntity version;

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private AIAnalysisEntity aiAnalysis;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParsedDataEntity> parsedData;

    // Constructeurs
    public FileEntity() {
    }

    public FileEntity(FileType fileType, String fileName, Integer versionId, Integer projectId) {
        this.fileType = fileType;
        this.fileName = fileName;
        this.versionId = versionId;
        this.projectId = projectId;
    }
}

