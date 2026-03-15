package lab.corekt.backend.modules.project.report.entity;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_analysis")
@EntityListeners(AuditingEntityListener.class)
public class AIAnalysisEntity {

    @Id
    @Column(name = "file_id")
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "file_id")
    private FileEntity file;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column
    private Integer qualityScore;

    @Column(columnDefinition = "TEXT") // replaced by text because could be too short [https://stackoverflow.com/a/22650941]
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String issues;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    public AIAnalysisEntity() {
    }

    public AIAnalysisEntity(FileEntity file) {
        this.file = file;
    }
}
