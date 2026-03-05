package lab.corekt.backend.modules.auth;

import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthContextTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthContext authContext;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should return user when authentication is valid")
    void testReturnsUserWhenAuthenticationIsValid() {
        UserEntity user = new UserEntity("user@test.com", "testuser");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("user@test.com", null, Collections.emptyList())
        );
        when(userService.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThat(authContext.getCurrentUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should throw when no authentication is present in security context")
    void testThrowsWhenNoAuthenticationPresent() {
        assertThatThrownBy(() -> authContext.getCurrentUser())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No authenticated user");
    }

    @Test
    @DisplayName("Should throw when authenticated email has no matching user in database")
    void testThrowsWhenAuthenticatedEmailHasNoMatchingUser() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ghost@test.com", null, Collections.emptyList())
        );
        when(userService.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authContext.getCurrentUser())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ghost@test.com");
    }
}