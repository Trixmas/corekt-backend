package lab.corekt.backend.modules.mistral.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetectedUserStory {

    private String id;
    private String actor;
    private String action;
    private String benefit;
    private String confidence;
    private String sourceExcerpt;
}
