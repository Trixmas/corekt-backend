package lab.corekt.backend.modules.taiga.controller;
import lab.corekt.backend.modules.auth.service.JwtService;
import lab.corekt.backend.modules.taiga.dto.TaigaCredentials;
import lab.corekt.backend.modules.taiga.exception.TaigaAuthException;
import lab.corekt.backend.modules.taiga.service.TaigaService;
import lab.corekt.backend.modules.taiga.service.TaigaTokenService;
import lab.corekt.backend.utils.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaigaAuthController.class)
class TaigaAuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TaigaTokenService taigaTokenService;

    @MockitoBean
    private TaigaService taigaService;

    @Test
    @WithMockUser
    @DisplayName("POST /taiga/login returns 204 and stores Taiga credentials")
    void testLoginReturns204AndStoresCredentials() throws Exception {
        when(taigaService.authenticate("alice", "secret"))
            .thenReturn(new TaigaCredentials("token-123", "42"));

        mockMvc.perform(post("/taiga/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"secret"}
                    """))
            .andExpect(status().isNoContent());

        verify(taigaService).authenticate("alice", "secret");
        verify(taigaTokenService).store("token-123", "42");
    }

    @Test
    @WithMockUser
    @DisplayName("POST /taiga/login returns 400 when payload is invalid")
    void testLoginReturns400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/taiga/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"","password":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.errors.username").value("Username is required"))
            .andExpect(jsonPath("$.errors.password").value("Password is required"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /taiga/projects returns wrapped project map")
    void testGetProjectsReturnsWrappedProjectMap() throws Exception {
        Map<String, String> projects = new LinkedHashMap<>();
        projects.put("Project A", "1");
        projects.put("Project B", "2");

        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(taigaTokenService.getUserId()).thenReturn("42");
        when(taigaService.fetchProjects("token-123", "42")).thenReturn(projects);

        mockMvc.perform(get("/taiga/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projects['Project A']").value("1"))
            .andExpect(jsonPath("$.projects['Project B']").value("2"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /taiga/projects returns 401 when Taiga token is invalid")
    void testGetProjectsReturns401WhenTaigaTokenIsInvalid() throws Exception {
        when(taigaTokenService.getToken()).thenReturn("token-123");
        when(taigaTokenService.getUserId()).thenReturn("42");
        when(taigaService.fetchProjects("token-123", "42"))
            .thenThrow(new TaigaAuthException("Taiga token is invalid or expired"));

        mockMvc.perform(get("/taiga/projects"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Taiga token is invalid or expired"));
    }
}
