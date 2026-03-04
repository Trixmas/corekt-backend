package lab.corekt.backend.modules.project.file.taiga.dto;

/**
 * DTO pour la réponse de login Taiga
 * Ne contient plus le token (stocké côté serveur)
 */
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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

