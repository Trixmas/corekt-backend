package lab.corekt.backend.modules.project.file.taiga.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private boolean ok = true;

    public LoginResponse(boolean ok) {
        this.ok = ok;
    }

    public LoginResponse() {

    }
}