package lab.corekt.backend.modules.mistral.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewSuggestion {

    private String id;
    private String category;
    private String title;
    private String question;
    private String priority;
    private String rationale;
    private List<String> relatedIssues;
}
