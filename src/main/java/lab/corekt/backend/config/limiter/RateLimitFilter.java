package lab.corekt.backend.config.limiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lab.corekt.backend.utils.error.ErrorResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> bucketCache;
    private final ObjectMapper objectMapper;
    private final int requests;
    private final int durationMinutes;

    public RateLimitFilter(
        ObjectMapper objectMapper,
        @Value("${ratelimit.requests:100}") int requests,
        @Value("${ratelimit.duration-minutes:1}") int durationMinutes) {
        this.objectMapper = objectMapper;
        this.requests = requests;
        this.durationMinutes = durationMinutes;
        this.bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        String clientIp = getClientIp(request);
        Bucket bucket = bucketCache.get(clientIp, _ -> createNewBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        handleRateLimitExceeded(request, response);
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(requests)
            .refillGreedy(requests, Duration.ofMinutes(durationMinutes))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "Too many requests. Please try again later.",
            null,
            request.getRequestURI(),
            Instant.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
