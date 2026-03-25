package lab.corekt.backend.config.limiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final TestController controller = new TestController();

    @Test
    @DisplayName("Should ignore handlers without @RateLimit")
    void testIgnoresHandlersWithoutRateLimitAnnotation() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(objectMapper);
        HandlerMethod handler = new HandlerMethod(controller, TestController.class.getMethod("unlimited"));

        MockHttpServletRequest firstRequest = buildRequest("GET", "/unlimited", "127.0.0.1");
        MockHttpServletRequest secondRequest = buildRequest("GET", "/unlimited", "127.0.0.1");

        boolean firstAllowed = interceptor.preHandle(firstRequest, new MockHttpServletResponse(), handler);
        boolean secondAllowed = interceptor.preHandle(secondRequest, new MockHttpServletResponse(), handler);

        assertThat(firstAllowed).isTrue();
        assertThat(secondAllowed).isTrue();
    }

    @Test
    @DisplayName("Should return 429 when an annotated endpoint rate limit is exceeded")
    void testReturns429WhenAnnotatedEndpointRateLimitIsExceeded() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(objectMapper);
        HandlerMethod handler = new HandlerMethod(controller, TestController.class.getMethod("limited"));

        MockHttpServletRequest firstRequest = buildRequest("POST", "/auth/login", "127.0.0.1");
        MockHttpServletRequest secondRequest = buildRequest("POST", "/auth/login", "127.0.0.1");
        MockHttpServletRequest thirdRequest = buildRequest("POST", "/auth/login", "127.0.0.1");

        boolean firstAllowed = interceptor.preHandle(firstRequest, new MockHttpServletResponse(), handler);
        boolean secondAllowed = interceptor.preHandle(secondRequest, new MockHttpServletResponse(), handler);

        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();
        boolean thirdAllowed = interceptor.preHandle(thirdRequest, thirdResponse, handler);

        assertThat(firstAllowed).isTrue();
        assertThat(secondAllowed).isTrue();
        assertThat(thirdAllowed).isFalse();
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getContentType()).isEqualTo("application/json");
        assertThat(thirdResponse.getContentAsString()).contains("\"status\":429");
        assertThat(thirdResponse.getContentAsString()).contains("Too many requests. Please try again later.");
        assertThat(thirdResponse.getContentAsString()).contains("\"path\":\"/auth/login\"");
    }

    @Test
    @DisplayName("Should keep separate rate limit buckets for each request signature")
    void testKeepsSeparateRateLimitBucketsForEachRequestSignature() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(objectMapper);
        HandlerMethod handler = new HandlerMethod(controller, TestController.class.getMethod("singleRequestLimit"));

        MockHttpServletRequest getRequest = buildRequest("GET", "/auth/login", "127.0.0.1");
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        boolean getAllowed = interceptor.preHandle(getRequest, getResponse, handler);

        MockHttpServletRequest postRequest = buildRequest("POST", "/auth/login", "127.0.0.1");
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        boolean postAllowed = interceptor.preHandle(postRequest, postResponse, handler);

        MockHttpServletRequest secondGetRequest = buildRequest("GET", "/auth/login", "127.0.0.1");
        MockHttpServletResponse secondGetResponse = new MockHttpServletResponse();
        boolean secondGetAllowed = interceptor.preHandle(secondGetRequest, secondGetResponse, handler);

        assertThat(getAllowed).isTrue();
        assertThat(postAllowed).isTrue();
        assertThat(secondGetAllowed).isFalse();
        assertThat(secondGetResponse.getStatus()).isEqualTo(429);
    }

    private MockHttpServletRequest buildRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    static class TestController {

        @GetMapping("/unlimited")
        public void unlimited() {
        }

        @PostMapping("/auth/login")
        @RateLimit(requests = 2)
        public void limited() {
        }

        @GetMapping("/auth/login")
        @RateLimit(requests = 1)
        public void singleRequestLimit() {
        }
    }
}
