package lab.corekt.backend.modules.project.file.taiga.dto;

public record LoginResponse(boolean success, String message) {
    public LoginResponse() {
        this(false, "");
    }

}

