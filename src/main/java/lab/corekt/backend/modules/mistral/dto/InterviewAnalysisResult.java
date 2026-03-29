package lab.corekt.backend.modules.mistral.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewAnalysisResult {

    private Integer qualityScore;
    private String summary;
    private List<DetectedUserStory> detectedUserStories;
    private QqoqcpDetails qqoqcpAnalysis;
    private DetectedIssues detectedIssues;
    private List<InterviewSuggestion> suggestions;
}
