package lab.corekt.backend.modules.mistral.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InterviewSuggestion {

    private String id;
    private String category;
    private String title;
    private String question;
    private String priority;
    private String rationale;
    private List<String> relatedIssues;
}
