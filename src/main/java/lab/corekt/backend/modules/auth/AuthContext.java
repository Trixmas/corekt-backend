package lab.corekt.backend.modules.auth;

import lab.corekt.backend.modules.user.entity.UserEntity;
import lab.corekt.backend.modules.user.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public final class AuthContext {

    private final UserService userService;

    /**
     * Resolves the {@link UserEntity} associated with the JWT in the current request's
     * {@code Authorization} header.
     *
     * <p>By the time this method is invoked, {@link lab.corekt.backend.modules.auth.security.JwtAuthenticationFilter}
     * has already validated the token's signature and expiry, and populated the
     * {@link org.springframework.security.core.context.SecurityContext}. The email
     * extracted here is therefore cryptographically verified and trustworthy.
     *
     * <p>The subsequent DB lookup also acts as a revocation check — if the user
     * no longer exists, this method throws regardless of token validity.
     *
     * <pre>{@code
     * @GetMapping("/demonstration")
     * public ResponseEntity<MyResource> getMyResource() {
     *     UserEntity currentUser = authContext.getCurrentUser();
     *     return ResponseEntity.ok(myService.getResourceFor(currentUser));
     * }
     * }</pre>
     *
     * @return the authenticated {@link UserEntity} for this request
     * @throws IllegalStateException if no authentication is present in the security context,
     *                               or if the authenticated email does not match any user in the database
     */
    public UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user in current request context");
        }

        String email = authentication.getName();
        return userService.findByEmail(email).orElseThrow(() -> new IllegalStateException("Authenticated user not found in database: " + email));
    }
}
