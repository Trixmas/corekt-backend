package lab.corekt.backend.modules.mistral.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QqoqcpDimensionDetailed {

    private Boolean covered;
    private String coverage;
    private List<String> elementsFound;
    private List<String> elementsMissing;
    private String explanation;
}
