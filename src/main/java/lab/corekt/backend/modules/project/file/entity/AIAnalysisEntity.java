package lab.corekt.backend.modules.project.file.entity;

import jakarta.persistence.*;
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

    @Column(length = 500)
    private String issues;

    @Column(length = 500)
    private String suggestions;

    public AIAnalysisEntity() {
    }

    public AIAnalysisEntity(FileEntity file) {
        this.file = file;
    }
}

