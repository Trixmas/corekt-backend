package lab.corekt.backend.modules.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private KeyPair rsaKeyPair;
    private String base64MasterKey;
    private String base64PrivateKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a fresh 256-bit master key
        byte[] masterKeyBytes = new byte[32];
        new SecureRandom().nextBytes(masterKeyBytes);
        base64MasterKey = Base64.getEncoder().encodeToString(masterKeyBytes);

        // Generate a fresh RSA keypair for tests
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        rsaKeyPair = keyPairGenerator.generateKeyPair();

        base64PrivateKey = Base64.getEncoder().encodeToString(rsaKeyPair.getPrivate().getEncoded());
        encryptionService = new EncryptionService(base64MasterKey, base64PrivateKey);
    }

    @Test
    @DisplayName("encryptForStorage produces a non-blank iv:ciphertext string")
    void testEncryptForStorageProducesValidFormat() throws Exception {
        String result = encryptionService.encryptForStorage("sk-test-key");

        assertThat(result).contains(":");
        String[] parts = result.split(":");
        assertThat(parts).hasSize(2);
        assertThat(parts[0]).isNotBlank();
        assertThat(parts[1]).isNotBlank();
    }

    @Test
    @DisplayName("decryptFromStorage recovers the original plaintext")
    void testDecryptFromStorageRecoversPlaintext() throws Exception {
        String plaintext = "sk-test-mistral-key";
        String stored = encryptionService.encryptForStorage(plaintext);

        String result = encryptionService.decryptFromStorage(stored);

        assertThat(result).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encryptForStorage produces different ciphertext each call due to random IV")
    void testEncryptForStorageIsNonDeterministic() throws Exception {
        String first = encryptionService.encryptForStorage("sk-test-key");
        String second = encryptionService.encryptForStorage("sk-test-key");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("decryptFromStorage throws when stored value format is invalid")
    void testDecryptFromStorageThrowsOnInvalidFormat() {
        assertThatThrownBy(() -> encryptionService.decryptFromStorage("notvalidformat"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid stored encryption format");
    }

    @Test
    @DisplayName("decryptAndReEncryptApiKey recovers and re-encrypts the original API key")
    void testDecryptAndReEncryptApiKeyRoundTrip() throws Exception {
        String originalApiKey = "sk-original-mistral-key";

        // Simulate what the frontend does
        String[] frontendPayload = simulateFrontendEncryption(originalApiKey, rsaKeyPair);
        String base64EncryptedApiKey = frontendPayload[0];
        String base64EncryptedDataKey = frontendPayload[1];
        String base64Iv = frontendPayload[2];

        String stored = encryptionService.decryptAndReEncryptApiKey(
            base64EncryptedApiKey, base64EncryptedDataKey, base64Iv
        );

        // Verify the stored value decrypts back to the original
        String recovered = encryptionService.decryptFromStorage(stored);
        assertThat(recovered).isEqualTo(originalApiKey);
    }

    @Test
    @DisplayName("decryptAndReEncryptApiKey throws when encrypted data key is tampered")
    void testDecryptAndReEncryptApiKeyThrowsOnTamperedDataKey() throws Exception {
        String[] frontendPayload = simulateFrontendEncryption("sk-key", rsaKeyPair);

        assertThatThrownBy(() -> encryptionService.decryptAndReEncryptApiKey(
            frontendPayload[0],
            Base64.getEncoder().encodeToString("tampered".getBytes()),
            frontendPayload[2]
        )).isInstanceOf(Exception.class);
    }


    @Test
    @DisplayName("constructor throws when master key is not 256 bits")
    void testConstructorThrowsOnInvalidMasterKeySize() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new EncryptionService(shortKey, base64PrivateKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Master key must be 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("constructor throws when RSA private key is invalid")
    void testConstructorThrowsOnInvalidPrivateKey() {
        String invalidKey = Base64.getEncoder().encodeToString("notavalidkey".getBytes());

        assertThatThrownBy(() -> new EncryptionService(base64MasterKey, invalidKey))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load RSA private key");
    }

    private String[] simulateFrontendEncryption(String apiKey, KeyPair keyPair) throws Exception {
        // Generate AES data key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey dataKey = keyGen.generateKey();

        // Encrypt API key with AES-GCM
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.ENCRYPT_MODE, dataKey, new GCMParameterSpec(128, iv));
        byte[] encryptedApiKey = aesCipher.doFinal(apiKey.getBytes());

        // Wrap data key with RSA public key (OAEP SHA-256)
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), oaepParams);
        byte[] encryptedDataKey = rsaCipher.doFinal(dataKey.getEncoded());

        return new String[]{
            Base64.getEncoder().encodeToString(encryptedApiKey),
            Base64.getEncoder().encodeToString(encryptedDataKey),
            Base64.getEncoder().encodeToString(iv)
        };
    }
}
