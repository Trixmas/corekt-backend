package lab.corekt.backend.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.corekt.backend.modules.auth.AuthContext;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.user.dto.ApiKeyRequest;
import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthContext authContext;

    private UserEntity currentUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        currentUser = new UserEntity("user@example.com", "testUser");
        currentUser.setId(UUID.randomUUID());
        when(authContext.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("GET /user/me returns 200 with user details")
    void testGetAuthenticatedUserReturns200() throws Exception {
        mockMvc.perform(get("/user/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    @DisplayName("POST /user/me/api-key returns 204 when request is valid")
    void testSaveApiKeyReturns204() throws Exception {
        ApiKeyRequest request = new ApiKeyRequest();
        request.setEncryptedApiKey("encApiKey");
        request.setEncryptedDataKey("encDataKey");
        request.setInitializationVector("iv");

        mockMvc.perform(post("/user/me/api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(userService).saveApiKey(eq(currentUser), any(ApiKeyRequest.class));
    }

    @Test
    @DisplayName("POST /user/me/api-key returns 400 when request body is invalid")
    void testSaveApiKeyReturns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/user/me/api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /user/{userId} returns 204 when user deletes own account")
    void testDeleteAccountReturns204() throws Exception {
        UUID userId = currentUser.getId();

        mockMvc.perform(delete("/user/" + userId))
            .andExpect(status().isNoContent());

        verify(userService).deleteById(eq(userId), eq(currentUser));
    }

    @Test
    @DisplayName("DELETE /user/{userId} returns 400 when user attempts to delete another account")
    void testDeleteAccountReturns400WhenIdMismatch() throws Exception {
        UUID differentId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Cannot delete another user's account"))
            .when(userService).deleteById(eq(differentId), eq(currentUser));

        mockMvc.perform(delete("/user/" + differentId))
            .andExpect(status().isBadRequest());
    }
}
