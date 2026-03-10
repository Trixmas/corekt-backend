package lab.corekt.backend.modules.upload.taiga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.upload.taiga.dto.LoginRequest;
import lab.corekt.backend.modules.upload.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.upload.taiga.service.TaigaService;
import lab.corekt.backend.modules.upload.taiga.service.TaigaTokenService;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TaigaAuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class TaigaAuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TaigaTokenService taigaTokenService;

    @MockitoBean
    private TaigaService taigaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /taiga/login returns 200 and stores token on success")
    void loginToTaiga_returns200_whenAuthenticationSucceeds() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(taigaService.authenticate("alice", "secret"))
                .thenReturn(new TaigaCredentials("token-123", "42"));

        mockMvc.perform(post("/taiga/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Successfully logged in to Taiga")));

        verify(taigaService, times(1)).authenticate("alice", "secret");
        verify(taigaTokenService).storeTaigaToken("token-123", "42");
    }

    @Test
    @DisplayName("POST /taiga/login returns 500 on Taiga error")
    void loginToTaiga_returns500_whenAuthenticationFails() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        when(taigaService.authenticate("alice", "secret"))
            .thenThrow(new RuntimeException("Internal Server Error"));

        mockMvc.perform(post("/taiga/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Internal Server Error")));

        verify(taigaTokenService, never()).storeTaigaToken(anyString(), anyString());
    }

    @Test
    @DisplayName("GET /taiga/projects returns 401 when user is not authenticated with Taiga")
    void getProjects_returns401_whenNoTaigaToken() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn(null);
        when(taigaTokenService.getTaigaUserId()).thenReturn(null);

        mockMvc.perform(get("/taiga/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Not authenticated with Taiga")));
    }

    @Test
    @DisplayName("GET /taiga/projects returns 200 with projects")
    void getProjects_returns200_whenAuthenticated() throws Exception {
        Map<String, String> projects = new LinkedHashMap<>();
        projects.put("Project A", "1");
        projects.put("Project B", "2");

        when(taigaTokenService.getTaigaToken()).thenReturn("token-123");
        when(taigaTokenService.getTaigaUserId()).thenReturn("42");
        when(taigaService.fetchProjects("token-123", "42")).thenReturn(projects);

        mockMvc.perform(get("/taiga/projects"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Project A")))
                .andExpect(content().string(containsString("Project B")));
    }

    @Test
    @DisplayName("GET /taiga/projects returns 500 when TaigaService fails")
    void getProjects_returns500_whenServiceFails() throws Exception {
        when(taigaTokenService.getTaigaToken()).thenReturn("token-123");
        when(taigaTokenService.getTaigaUserId()).thenReturn("42");
        when(taigaService.fetchProjects("token-123", "42"))
                .thenThrow(new RuntimeException("Internal Server Error"));

        mockMvc.perform(get("/taiga/projects"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to fetch projects from Taiga")))
                .andExpect(content().string(containsString("Internal Server Error")));
    }
}