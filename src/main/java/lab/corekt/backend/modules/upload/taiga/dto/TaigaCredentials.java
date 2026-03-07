package lab.corekt.backend.modules.upload.taiga.dto;

/**
 * Holds the Taiga authentication token and user identifier returned after a
 * successful login.
 */
public record TaigaCredentials(String token, String userId) {
}
