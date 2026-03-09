package lab.corekt.backend.modules.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKeySpec masterKey;
    private final RSAPrivateKey rsaPrivateKey;

    public EncryptionService(
            @Value("${encryption.master-key}") String base64MasterKey,
            @Value("${encryption.rsa-private-key}") String base64RsaPrivateKey
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(base64MasterKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Master key must be 256 bits (32 bytes)");
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
        this.rsaPrivateKey = loadRsaPrivateKey(base64RsaPrivateKey);
    }

    private byte[] unwrapDataKey(String base64EncryptedDataKey) throws Exception {
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        Cipher rsaCipher = Cipher.getInstance(RSA_ALGORITHM);
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaepParams);
        return rsaCipher.doFinal(Base64.getDecoder().decode(base64EncryptedDataKey));
    }

    private String decryptWithDataKey(String base64EncryptedApiKey, String base64Iv, byte[] dataKeyBytes) throws Exception {
        SecretKeySpec dataKey = new SecretKeySpec(dataKeyBytes, "AES");
        byte[] iv = Base64.getDecoder().decode(base64Iv);
        byte[] ciphertext = Base64.getDecoder().decode(base64EncryptedApiKey);

        Cipher aesCipher = Cipher.getInstance(AES_ALGORITHM);
        aesCipher.init(Cipher.DECRYPT_MODE, dataKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return new String(aesCipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    public String encryptForStorage(String plaintext) throws Exception {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(ciphertext);
    }

    public String decryptFromStorage(String storedValue) throws Exception {
        String[] parts = storedValue.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid stored encryption format");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    public String decryptAndReEncryptApiKey(
            String base64EncryptedApiKey,
            String base64EncryptedDataKey,
            String base64Iv
    ) throws Exception {
        byte[] dataKeyBytes = unwrapDataKey(base64EncryptedDataKey);
        String plaintextApiKey = decryptWithDataKey(base64EncryptedApiKey, base64Iv, dataKeyBytes);
        return encryptForStorage(plaintextApiKey);
    }

    private RSAPrivateKey loadRsaPrivateKey(String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim().replaceAll("\\s+", ""));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }
}
