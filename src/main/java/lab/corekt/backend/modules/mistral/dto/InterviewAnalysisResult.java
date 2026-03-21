package lab.corekt.backend.modules.mistral.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InterviewAnalysisResult {

    private Integer qualityScore;
    private String summary;
    private List<DetectedUserStory> detectedUserStories;
    private QqoqcpDetails qqoqcpAnalysis;
    private DetectedIssues detectedIssues;
    private List<InterviewSuggestion> suggestions;
}
