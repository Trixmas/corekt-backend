package lab.corekt.backend.modules.upload.taiga.dto;

public record LoginResponse(boolean success, String message) {
    public LoginResponse() {
        this(false, "");
    }

}

