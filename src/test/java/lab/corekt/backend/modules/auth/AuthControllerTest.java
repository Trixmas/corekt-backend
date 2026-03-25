package lab.corekt.backend.modules.auth;

import lab.corekt.backend.modules.auth.dto.LdapUserProfile;
import lab.corekt.backend.modules.auth.service.JumpCloudService;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JumpCloudService jumpCloudService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Should return 401 when LDAP authentication fails")
    void testReturns401WhenLdapAuthenticationFails() throws Exception {
        when(jumpCloudService.authenticateAndGetProfile(anyString(), anyString()))
            .thenReturn(null);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 200 with token when credentials are valid")
    void testReturns200WithTokenWhenCredentialsAreValid() throws Exception {
        LdapUserProfile ldapProfile = new LdapUserProfile();
        ldapProfile.setEmail("user@test.com");
        ldapProfile.setUsername("testuser");

        UserEntity user = new UserEntity("user@test.com", "testuser");

        when(jumpCloudService.authenticateAndGetProfile("user@test.com", "password123"))
            .thenReturn(ldapProfile);
        when(userService.createOrUpdateUser("user@test.com", "testuser"))
            .thenReturn(user);
        when(jwtService.generateToken(anyString(), any(Map.class), anyLong()))
            .thenReturn("mocked-jwt-token");
        when(jwtService.computeExpirationTime(anyLong()))
            .thenReturn(9999999999L);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "user@test.com", "password": "password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
            .andExpect(jsonPath("$.expiresAt").value(9999999999L))
            .andExpect(jsonPath("$.user.email").value("user@test.com"))
            .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void testReturns400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "not-an-email", "password": "password123"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when request body is missing required fields")
    void testReturns400WhenRequestBodyIsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}