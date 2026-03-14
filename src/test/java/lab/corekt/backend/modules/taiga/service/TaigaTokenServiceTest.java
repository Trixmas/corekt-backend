package lab.corekt.backend.modules.taiga.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class TaigaTokenServiceTest {

    private final TaigaTokenService taigaTokenService = new TaigaTokenService();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("store() and getters return correct token and userId for authenticated user")
    void testStoreAndGettersForAuthenticatedUser() {
        authenticate("alice@example.com", true);

        taigaTokenService.store("token-alice", "taiga-user-alice");

        assertEquals("token-alice", taigaTokenService.getToken());
        assertEquals("taiga-user-alice", taigaTokenService.getUserId());
    }

    @Test
    @DisplayName("getToken() and getUserId() return null when no token stored for authenticated user")
    void testGettersReturnNullWhenNoTokenStored() {
        authenticate("alice@example.com", true);

        assertNull(taigaTokenService.getToken());
        assertNull(taigaTokenService.getUserId());
    }

    @Test
    @DisplayName("store() throws IllegalStateException when authentication is null")
    void testStoreThrowsWhenAuthenticationIsNull() {
        setAuthentication(null);

        assertThrows(IllegalStateException.class, () -> taigaTokenService.store("token", "taiga-user"));
    }

    @Test
    @DisplayName("getToken() throws IllegalStateException when authentication is null")
    void testGetTokenThrowsWhenAuthenticationIsNull() {
        setAuthentication(null);

        assertThrows(IllegalStateException.class, taigaTokenService::getToken);
    }

    @Test
    @DisplayName("getUserId() throws IllegalStateException when authentication is null")
    void testGetUserIdThrowsWhenAuthenticationIsNull() {
        setAuthentication(null);

        assertThrows(IllegalStateException.class, taigaTokenService::getUserId);
    }

    @Test
    @DisplayName("store() throws IllegalStateException when user is not authenticated")
    void testStoreThrowsForUnauthenticatedUser() {
        authenticate("bob@example.com", false);

        assertThrows(IllegalStateException.class, () -> taigaTokenService.store("token-bob", "taiga-user-bob"));
    }

    @Test
    @DisplayName("remove() deletes only the current user's token, leaving other users unaffected")
    void testRemoveDeletesOnlyCurrentUserToken() {
        authenticate("alice@example.com", true);
        taigaTokenService.store("token-alice", "taiga-user-alice");

        authenticate("bob@example.com", true);
        taigaTokenService.store("token-bob", "taiga-user-bob");

        authenticate("alice@example.com", true);
        taigaTokenService.remove();

        assertNull(taigaTokenService.getToken());
        assertNull(taigaTokenService.getUserId());

        authenticate("bob@example.com", true);
        assertEquals("token-bob", taigaTokenService.getToken());
        assertEquals("taiga-user-bob", taigaTokenService.getUserId());
    }

    @Test
    @DisplayName("remove() throws IllegalStateException when authentication is null")
    void testRemoveThrowsWhenAuthenticationIsNull() {
        setAuthentication(null);

        assertThrows(IllegalStateException.class, taigaTokenService::remove);
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
}
