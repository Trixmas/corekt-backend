package lab.corekt.backend.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lab.corekt.backend.config.WebConfig;
import lab.corekt.backend.config.limiter.RateLimitFilter;
import lab.corekt.backend.config.limiter.RateLimitInterceptor;
import org.jspecify.annotations.NonNull;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Base class for all {@code @WebMvcTest} controller tests.
 *
 * <p>Stubs out infrastructure components that are registered in the MVC filter/interceptor
 * chain ({@link RateLimitFilter}, {@link RateLimitInterceptor}, {@link WebConfig}) so that
 * requests reach the controller under test without interference.
 *
 * <p>Subclasses must be annotated with {@code @WebMvcTest(SomeController.class)} and may
 * declare additional {@code @MockitoBean} fields for their own service dependencies.
 */
@Import(BaseControllerTest.MockConfig.class)
public abstract class BaseControllerTest {

    @MockitoBean
    private WebConfig webConfig;

    @TestConfiguration
    static class MockConfig implements WebMvcConfigurer {

        @Bean
        public RateLimitFilter rateLimitFilter() throws Exception {
            RateLimitFilter mock = Mockito.mock(RateLimitFilter.class);
            Mockito.doAnswer(invocation -> {
                HttpServletRequest request = invocation.getArgument(0);
                HttpServletResponse response = invocation.getArgument(1);
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(request, response);
                return null;
            }).when(mock).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
            return mock;
        }

        @Bean
        public RateLimitInterceptor rateLimitInterceptor() throws Exception {
            RateLimitInterceptor mock = Mockito.mock(RateLimitInterceptor.class);
            Mockito.when(mock.preHandle(
                Mockito.any(), Mockito.any(), Mockito.any()
            )).thenReturn(true);
            return mock;
        }

        @Override
        public void addInterceptors(@NonNull InterceptorRegistry registry) {
            // no interceptors in tests
        }
    }
}
