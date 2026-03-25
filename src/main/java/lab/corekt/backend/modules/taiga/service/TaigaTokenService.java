package lab.corekt.backend.modules.taiga.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TaigaTokenService {

    private final Cache<String, TaigaTokenData> tokenCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();

    public void store(String token, String userId) {
        String userEmail = resolveCurrentUserEmail();
        tokenCache.put(userEmail, new TaigaTokenData(token, userId));
    }

    public String getToken() {
        TaigaTokenData data = tokenCache.getIfPresent(resolveCurrentUserEmail());
        return data != null ? data.token() : null;
    }

    public String getUserId() {
        TaigaTokenData data = tokenCache.getIfPresent(resolveCurrentUserEmail());
        return data != null ? data.userId() : null;
    }

    public void remove() {
        tokenCache.invalidate(resolveCurrentUserEmail());
    }

    private String resolveCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return authentication.getName();
    }

    private record TaigaTokenData(String token, String userId) {
    }
}
