package lab.corekt.backend.modules.upload.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueFileTypeForVersion", columnNames = {"version_id", "file_type"})
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
    @JoinColumn(name = "version_id", referencedColumnName = "version_id")
    private VersionEntity version;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private AIAnalysisEntity aiAnalysis;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParsedDataEntity> parsedData = new ArrayList<>();

    // Constructeurs
    public FileEntity() {
    }

    public FileEntity(FileType fileType, String fileName) {
        this.type = fileType;
        this.name = fileName;
    }
}

