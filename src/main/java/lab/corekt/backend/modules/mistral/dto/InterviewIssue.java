package lab.corekt.backend.modules.mistral.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewIssue {

    private String id;
    private String excerpt;
    private String description;
    private String severity;
    private String impact;
}
