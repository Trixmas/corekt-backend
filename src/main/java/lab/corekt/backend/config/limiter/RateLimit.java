package lab.corekt.backend.config.limiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts the number of requests a single IP can make to the annotated endpoint.
 * Overrides the global rate limit defined in application properties.
 *
 * <p>Usage example:
 * <pre>{@code
 * @RateLimit(requests = 5)
 * @PostMapping("/login")
 * public ResponseEntity<?> login(...) { ... }
 *
 * @RateLimit(requests = 3, durationMinutes = 10)
 * @PostMapping("/register")
 * public ResponseEntity<?> register(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * Maximum number of requests allowed per IP within the duration window.
     */
    int requests();

    /**
     * Duration window in minutes. Defaults to 1 minute.
     */
    int durationMinutes() default 1;
}
