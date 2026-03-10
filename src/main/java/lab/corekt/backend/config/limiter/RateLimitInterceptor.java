package lab.corekt.backend.config.limiter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lab.corekt.backend.utils.error.ErrorResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Cache<String, Bucket> bucketCache;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) return true;

        String cacheKey = request.getRemoteAddr() + ":" + request.getMethod() + ":" + request.getRequestURI();
        Bucket bucket = bucketCache.get(cacheKey, _ -> createBucket(rateLimit));

        if (bucket.tryConsume(1)) return true;

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
        return false;
    }

    private Bucket createBucket(RateLimit rateLimit) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(rateLimit.requests())
            .refillGreedy(rateLimit.requests(), Duration.ofMinutes(rateLimit.durationMinutes()))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
}
