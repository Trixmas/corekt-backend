package lab.corekt.backend.modules.project.versions.dto;

import lab.corekt.backend.modules.project.report.entity.AIAnalysisEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAnalysisResponse {
    private Integer qualityScore;
    private String summary;
    private String issues;
    private String suggestions;

    public AiAnalysisResponse() {
    }

    public AiAnalysisResponse(AIAnalysisEntity analysis) {
        this.qualityScore = analysis.getQualityScore();
        this.summary = analysis.getSummary();
        this.issues = analysis.getIssues();
        this.suggestions = analysis.getSuggestions();
    }
}

