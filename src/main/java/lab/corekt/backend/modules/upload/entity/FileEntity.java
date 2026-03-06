package lab.corekt.backend.modules.upload.entity;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "file", uniqueConstraints = {
        @UniqueConstraint(name = "UniqueFileTypeForVersion", columnNames = {"version_id", "project_id", "file_type"})
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
            @JoinColumn(name = "version_id", referencedColumnName = "version_id", nullable = false),
            @JoinColumn(name = "project_id", referencedColumnName = "project_id", nullable = false),
    })
    private VersionEntity version;

    @OneToOne(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private AIAnalysisEntity aiAnalysis;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParsedDataEntity> parsedData;

    public FileEntity() {
    }

    public FileEntity(FileType fileType, String fileName, VersionEntity version) {
        this.type = fileType;
        this.name = fileName;
        this.version = version;
    }
}

