package lab.corekt.backend.modules.upload.taiga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HTTPServiceTest {

    private HTTPService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.service = new HTTPService(builder);
    }

    @Test
    @DisplayName("get sends bearer authorization header")
    void testGetSendsAuthorizationHeader() {
        server.expect(requestTo("https://api.taiga.io/api/v1/projects"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
                .andRespond(withSuccess("{\"ok\": true}", MediaType.APPLICATION_JSON));

        String result = service.get("/projects", "token-123", null);

        assertThat(result).isEqualTo("{\"ok\": true}");
        server.verify();
    }

    @Test
    @DisplayName("get sends additional headers")
    void testGetSendsAdditionalHeaders() {
        server.expect(requestTo("https://api.taiga.io/api/v1/userstories?project=12"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
                .andExpect(header("x-disable-pagination", "True"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        String result = service.get(
                "/userstories?project=12",
                "token-123",
                Map.of("x-disable-pagination", "True")
        );

        assertThat(result).isEqualTo("[]");
        server.verify();
    }

    @Test
    @DisplayName("get works with null headers")
    void testGetWorksWithNullHeaders() {
        server.expect(requestTo("https://api.taiga.io/api/v1/projects?member=42"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-123"))
                .andRespond(withSuccess("[{\"id\":1}]", MediaType.APPLICATION_JSON));

        String result = service.get("/projects?member=42", "token-123", null);

        assertThat(result).isEqualTo("[{\"id\":1}]");
        server.verify();
    }

    @Test
    @DisplayName("post sends json body")
    void testPostSendsJsonBody() {
        String requestBody = """
                {"username":"alice","password":"secret"}
                """;

        server.expect(requestTo("https://api.taiga.io/api/v1/auth"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess("""
                        {"auth_token":"token-123","id":42}
                        """, MediaType.APPLICATION_JSON));

        String result = service.post("/auth", requestBody);

        assertThat(result).contains("auth_token");
        assertThat(result).contains("token-123");
        server.verify();
    }
}