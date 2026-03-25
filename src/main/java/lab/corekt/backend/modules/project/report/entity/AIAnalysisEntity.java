package lab.corekt.backend.modules.project.report.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

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

    @Column(columnDefinition = "TEXT")
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
