package lab.corekt.backend.modules.auth.dto;

import lab.corekt.backend.modules.user.dto.UserDetails;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String token;
    private UserDetails user;
    private Long expiresAt;
}
