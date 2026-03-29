package lab.corekt.backend.modules.mistral.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetectedIssues {

    private List<InterviewIssue> evasiveAnswers;
    private List<InterviewIssue> contradictions;
    private List<InterviewIssue> missingCriticalInfo;
    private List<InterviewIssue> ambiguities;
    private List<InterviewIssue> redFlags;
}
