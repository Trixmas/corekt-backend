package lab.corekt.backend.modules.project.file.taiga.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UsResponse {
    private List<US> usList;
    private boolean success;
    private String message;

    public UsResponse(List<US> usList) {
        this.usList = usList;
        this.success = usList != null;
        this.message = null;
    }

    public UsResponse(List<US> usList, boolean success, String errorMessage) {
        this.usList = usList;
        this.success = success;
        this.message = errorMessage;
    }

}
