package lab.corekt.backend.modules.mistral.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewIssue {

    private String id;
    private String excerpt;
    private String description;
    private String severity;
    private String impact;
}
