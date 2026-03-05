package lab.corekt.backend.modules.auth.security;

import lab.corekt.backend.modules.auth.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip authentication when no Authorization header is present")
    void testSkipsAuthenticationWhenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should authenticate when a valid token is provided")
    void testAuthenticatesWhenValidTokenIsProvided() throws Exception {
        var claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("user@test.com");
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 3600000));
        when(jwtService.validateTokenSafe("valid-token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("Should not authenticate when an invalid token is provided")
    void testDoesNotAuthenticateWhenInvalidTokenIsProvided() throws Exception {
        when(jwtService.validateTokenSafe("bad-token")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}