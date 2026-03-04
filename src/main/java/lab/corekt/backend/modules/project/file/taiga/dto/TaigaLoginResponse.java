package lab.corekt.backend.modules.project.file.taiga.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO pour la réponse de login Taiga
 */
@Getter
@Setter
public class TaigaLoginResponse {
    private boolean success;
    private String message;

    public TaigaLoginResponse() {
        this.success = false;
        this.message = "";
    }

    public TaigaLoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

}

