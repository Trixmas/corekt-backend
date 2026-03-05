package lab.corekt.backend.modules.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "test-secret-key-that-is-long-enough-for-hs256");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Generated token should contain email as subject")
    void testGeneratedTokenContainsEmailSubject() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken("user@test.com", Map.of("email", "user@test.com"), issuedAt);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("Computed expiration time should match token expiry claim")
    void testComputedExpirationTimeMatchesTokenExpiryClaim() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken("user@test.com", Map.of(), issuedAt);
        Long expiresAt = jwtService.computeExpirationTime(issuedAt);

        var claims = jwtService.validateToken(token);
        assertThat(claims.getExpiration().getTime()).isEqualTo((expiresAt / 1000) * 1000);
    }

    @Test
    @DisplayName("Fresh token should not be expired")
    void testFreshTokenIsNotExpired() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken("user@test.com", Map.of(), issuedAt);

        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("Token with negative expiration should be expired")
    void testTokenWithNegativeExpirationIsExpired() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken("user@test.com", Map.of(), issuedAt);

        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    @DisplayName("Tampered token should return null on safe validation")
    void testTamperedTokenReturnsNullOnSafeValidation() {
        long issuedAt = System.currentTimeMillis();
        String token = jwtService.generateToken("user@test.com", Map.of(), issuedAt);

        assertThat(jwtService.validateTokenSafe(token + "tampered")).isNull();
    }
}