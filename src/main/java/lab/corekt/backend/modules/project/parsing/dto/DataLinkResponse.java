package lab.corekt.backend.modules.project.parsing.dto;

import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;

import java.util.UUID;

public record DataLinkResponse(
        UUID id,
        MatchType match,
        Severity severity,
        Integer confidenceScore,
        String justification,
        String suggestion,
        ParsedDataResponse sourceParsedData,
        ParsedDataResponse targetParsedData) {
    public DataLinkResponse(DataLinkEntity dataLink) {
        this(dataLink.getId(), dataLink.getMatch(), dataLink.getSeverity(), dataLink.getConfidenceScore(), dataLink.getJustification(), dataLink.getSuggestion(), new ParsedDataResponse(dataLink.getSourceParsedData()), new ParsedDataResponse(dataLink.getTargetParsedData()));
    }
}
