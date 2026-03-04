package lab.corekt.backend.modules.project.file.taiga.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service pour gérer le stockage en mémoire des tokens Taiga par utilisateur.
 * Les tokens sont stockés avec l'email de l'utilisateur comme clé.
 */
@Service
public class TaigaTokenService {

    private static final Map<String, TaigaTokenData> tokenCache = new HashMap<>();

    /**
     * Stocke le token Taiga pour l'utilisateur actuel
     *
     * @param token  le token Taiga
     * @param userId l'ID utilisateur Taiga
     */
    public void storeTaigaToken(String token, String userId) {
        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            tokenCache.put(userEmail, new TaigaTokenData(token, userId));
        }
    }

    /**
     * Récupère le token Taiga de l'utilisateur actuel
     *
     * @return le token Taiga ou null si non trouvé
     */
    public String getTaigaToken() {
        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            TaigaTokenData data = tokenCache.get(userEmail);
            return data != null ? data.token : null;
        }
        return null;
    }

    /**
     * Récupère l'ID utilisateur Taiga de l'utilisateur actuel
     *
     * @return l'ID utilisateur Taiga ou null si non trouvé
     */
    public String getTaigaUserId() {
        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            TaigaTokenData data = tokenCache.get(userEmail);
            return data != null ? data.userId : null;
        }
        return null;
    }

    /**
     * Supprime le token Taiga de l'utilisateur actuel
     */
    public void removeTaigaToken() {
        String userEmail = getCurrentUserEmail();
        if (userEmail != null) {
            tokenCache.remove(userEmail);
        }
    }

    /**
     * Récupère l'email de l'utilisateur actuellement authentifié
     *
     * @return l'email de l'utilisateur ou null si non authentifié
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
         * Classe interne pour stocker les données du token Taiga
         */
        private record TaigaTokenData(String token, String userId) {
    }
}

