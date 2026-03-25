package lab.corekt.backend.modules.project.parsing.entity;

import jakarta.persistence.*;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
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
    @Column()
    private Severity severity;

    @Column
    private Integer confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @ManyToOne
    @JoinColumn(name = "source_parsed_data_id", nullable = false)
    private ParsedDataEntity sourceParsedData;

    @ManyToOne
    @JoinColumn(name = "target_parsed_data_id", nullable = false)
    private ParsedDataEntity targetParsedData;

    // Constructeurs
    public DataLinkEntity() {
    }

    public DataLinkEntity(MatchType match, Severity severity, ParsedDataEntity sourceParsedData,
                          ParsedDataEntity targetParsedData) {
        this.match = match;
        this.severity = severity;
        this.sourceParsedData = sourceParsedData;
        this.targetParsedData = targetParsedData;
    }
}
