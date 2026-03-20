package lab.corekt.backend.config.limiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    @DisplayName("Should return 429 when the global IP rate limit is exceeded")
    void testReturns429WhenGlobalIpRateLimitIsExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, 2, 1);

        MockHttpServletRequest firstRequest = buildRequest("/auth/login", "127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilterInternal(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = buildRequest("/auth/login", "127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(secondRequest, secondResponse, new MockFilterChain());

        MockHttpServletRequest thirdRequest = buildRequest("/auth/login", "127.0.0.1");
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        filter.doFilterInternal(thirdRequest, thirdResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getContentType()).isEqualTo("application/json");
        assertThat(thirdResponse.getContentAsString()).contains("\"status\":429");
        assertThat(thirdResponse.getContentAsString()).contains("Too many requests. Please try again later.");
        assertThat(thirdResponse.getContentAsString()).contains("\"path\":\"/auth/login\"");
    }

    @Test
    @DisplayName("Should keep separate global rate limit buckets for different IP addresses")
    void testKeepsSeparateGlobalRateLimitBucketsForDifferentIpAddresses() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, 1, 1);

        MockHttpServletRequest blockedIpFirstRequest = buildRequest("/projects", "127.0.0.1");
        filter.doFilterInternal(blockedIpFirstRequest, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest blockedIpSecondRequest = buildRequest("/projects", "127.0.0.1");
        MockHttpServletResponse blockedIpSecondResponse = new MockHttpServletResponse();
        filter.doFilterInternal(blockedIpSecondRequest, blockedIpSecondResponse, new MockFilterChain());

        MockHttpServletRequest otherIpRequest = buildRequest("/projects", "127.0.0.2");
        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilterInternal(otherIpRequest, otherIpResponse, new MockFilterChain());

        assertThat(blockedIpSecondResponse.getStatus()).isEqualTo(429);
        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest buildRequest(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
