package lab.corekt.backend.modules.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LdapUserProfile {
    private String email;
    private String jumpCloudUid;
}
