package lab.corekt.backend.modules.project.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.enums.MatchType;
import lab.corekt.backend.modules.project.enums.Severity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "data_link")
@EntityListeners(AuditingEntityListener.class)
public class DataLinkEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "link_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column
    private Integer confidenceScore;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @ManyToOne
    @JoinColumn(name = "source_parsed_data_id", nullable = false, insertable = false, updatable = false)
    private ParsedDataEntity sourceParsedData;

    @Column(name = "source_parsed_data_id", nullable = false)
    private Integer sourceParsedDataId;

    @ManyToOne
    @JoinColumn(name = "target_parsed_data_id", nullable = false, insertable = false, updatable = false)
    private ParsedDataEntity targetParsedData;

    @Column(name = "target_parsed_data_id", nullable = false)
    private Integer targetParsedDataId;

    // Constructeurs
    public DataLinkEntity() {
    }

    public DataLinkEntity(MatchType match, Severity severity, Integer sourceParsedDataId, Integer targetParsedDataId) {
        this.match = match;
        this.severity = severity;
        this.sourceParsedDataId = sourceParsedDataId;
        this.targetParsedDataId = targetParsedDataId;
    }
}

