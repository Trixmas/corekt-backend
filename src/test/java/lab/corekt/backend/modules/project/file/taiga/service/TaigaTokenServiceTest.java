package lab.corekt.backend.modules.project.file.taiga.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TaigaTokenServiceTest {

    private final TaigaTokenService taigaTokenService = new TaigaTokenService();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        clearTokenCache();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearTokenCache();
    }

    @Test
    void storeTaigaTokenAndGetters_workForAuthenticatedUser() {
        authenticate("alice@example.com", true);

        taigaTokenService.storeTaigaToken("token-alice", "taiga-user-alice");

        assertEquals("token-alice", taigaTokenService.getTaigaToken());
        assertEquals("taiga-user-alice", taigaTokenService.getTaigaUserId());
    }

    @Test
    void getters_returnNullWhenNoTokenStoredForAuthenticatedUser() {
        authenticate("alice@example.com", true);

        assertNull(taigaTokenService.getTaigaToken());
        assertNull(taigaTokenService.getTaigaUserId());
    }

    @Test
    void storeTaigaToken_doesNothingWhenAuthenticationIsNull() {
        setAuthentication(null);

        taigaTokenService.storeTaigaToken("token", "taiga-user");

        assertEquals(0, tokenCacheSize());
    }

    @Test
    void getters_returnNullWhenAuthenticationIsNull() {
        setAuthentication(null);

        assertNull(taigaTokenService.getTaigaToken());
        assertNull(taigaTokenService.getTaigaUserId());
    }

    @Test
    void methods_ignoreUnauthenticatedUser() {
        authenticate("bob@example.com", false);

        taigaTokenService.storeTaigaToken("token-bob", "taiga-user-bob");

        assertNull(taigaTokenService.getTaigaToken());
        assertNull(taigaTokenService.getTaigaUserId());
        assertEquals(0, tokenCacheSize());
    }

    @Test
    void removeTaigaToken_removesOnlyCurrentUserToken() {
        authenticate("alice@example.com", true);
        taigaTokenService.storeTaigaToken("token-alice", "taiga-user-alice");

        authenticate("bob@example.com", true);
        taigaTokenService.storeTaigaToken("token-bob", "taiga-user-bob");

        authenticate("alice@example.com", true);
        taigaTokenService.removeTaigaToken();

        assertNull(taigaTokenService.getTaigaToken());
        assertNull(taigaTokenService.getTaigaUserId());

        authenticate("bob@example.com", true);
        assertEquals("token-bob", taigaTokenService.getTaigaToken());
        assertEquals("taiga-user-bob", taigaTokenService.getTaigaUserId());
    }

    @Test
    void removeTaigaToken_doesNothingWhenAuthenticationIsNull() {
        setAuthentication(null);

        taigaTokenService.removeTaigaToken();

        assertEquals(0, tokenCacheSize());
    }

    private void authenticate(String email, boolean isAuthenticated) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(email, "password");
        authentication.setAuthenticated(isAuthenticated);
        setAuthentication(authentication);
    }

    private void setAuthentication(TestingAuthenticationToken authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void clearTokenCache() {
        getTokenCache().clear();
    }

    private int tokenCacheSize() {
        return getTokenCache().size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getTokenCache() {
        try {
            Field tokenCacheField = TaigaTokenService.class.getDeclaredField("tokenCache");
            tokenCacheField.setAccessible(true);
            return (Map<String, ?>) tokenCacheField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to access TaigaTokenService token cache", e);
        }
    }
}