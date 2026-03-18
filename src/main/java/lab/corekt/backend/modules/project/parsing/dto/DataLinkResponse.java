package lab.corekt.backend.modules.project.parsing.dto;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DataLinkResponse {

    private UUID id;
    private MatchType match;
    private Severity severity;
    private Integer confidenceScore;
    private Integer score;
    private String justification;
    private String suggestion;

    public DataLinkResponse(DataLinkEntity dataLinkEntity) {
        this.id = dataLinkEntity.getId();
        this.match = dataLinkEntity.getMatch();
        this.severity = dataLinkEntity.getSeverity();
        this.confidenceScore = dataLinkEntity.getConfidenceScore();
        this.score = dataLinkEntity.getScore();
        this.justification = dataLinkEntity.getJustification();
        this.suggestion = dataLinkEntity.getSuggestion();
    }
}
