package lab.corekt.backend.modules.mistral.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewParsedDataResult {

	private List<String> actors;
	private List<String> actions;
	private List<String> flows;
	private List<DetectedUserStory> detectedUserStories;
}


