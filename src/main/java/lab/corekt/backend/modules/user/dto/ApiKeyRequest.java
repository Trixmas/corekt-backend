package lab.corekt.backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiKeyRequest {

    @NotBlank(message = "Encrypted API key is required")
    private String encryptedApiKey;

    @NotBlank(message = "Encrypted data key is required")
    private String encryptedDataKey;

    @NotBlank(message = "Initialization vector is required")
    private String initializationVector;
}
