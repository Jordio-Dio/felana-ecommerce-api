package com.friperie.felana.auth.service;

import com.friperie.felana.auth.domain.RefreshToken;
import com.friperie.felana.auth.domain.User;
import com.friperie.felana.auth.exception.TokenRefreshException;
import com.friperie.felana.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service dédié au cycle de vie du refresh token : création, vérification,
 * suppression. Contrairement à l'access token (JWT stateless géré par
 * JwtService), le refresh token est un simple identifiant opaque (UUID)
 * persisté en base, ce qui permet de le révoquer à tout moment.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Durée de vie du refresh token en millisecondes (ex: 604800000 = 7 jours). */
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Fenêtre de grâce en millisecondes pendant laquelle un refresh token déjà
     * tourné reste accepté (60 s par défaut, sans toucher aux fichiers de config).
     *
     * Sans elle, TOUT rafraîchissement concurrent tue la session : deux onglets
     * qui relancent le navigateur en même temps envoient le même refresh token,
     * le premier le fait tourner, le second arrive avec un token révoqué -> 401
     * -> déconnexion forcée. Idem si l'onglet est fermé pendant l'appel : la
     * rotation est commitée côté serveur mais la nouvelle valeur n'est jamais
     * écrite dans le stockage du client.
     */
    @Value("${jwt.refresh-rotation-grace-ms:60000}")
    private long rotationGraceMs;

    /**
     * Crée et persiste un nouveau refresh token pour l'utilisateur donné.
     * On ne réutilise jamais un token existant : chaque login génère un
     * nouveau refresh token (rotation), ce qui limite l'impact d'un vol.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Vérifie qu'un refresh token existe, n'est ni expiré ni révoqué.
     * Lève TokenRefreshException sinon (mappée en 401/403 côté contrôleur
     * ou gestionnaire d'exceptions global).
     */
    public RefreshToken verifyAndGet(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token introuvable."));

        if (refreshToken.isRevoked() && !isWithinRotationGrace(refreshToken)) {
            throw new TokenRefreshException("Refresh token révoqué. Veuillez vous reconnecter.");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token expiré. Veuillez vous reconnecter.");
        }

        return refreshToken;
    }

    /**
     * Rotation : à chaque utilisation du refresh token pour obtenir un nouvel
     * access token, on révoque l'ancien et on en émit un nouveau. Une réutilisation
     * au-delà de la fenêtre de grâce reste détectée comme un vol ; dans la fenêtre,
     * elle est simplement considérée comme un concurrent légitime (onglet,
     * réponse perdue) plutôt que comme une déconnexion à infliger.
     */
    @Transactional
    public RefreshToken rotate(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        oldToken.setRotatedAt(Instant.now());
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser());
    }

    /**
     * Un token révoqué par rotation reste accepté pendant rotationGraceMs.
     * rotatedAt == null signifie une révocation explicite : aucun sursis.
     */
    private boolean isWithinRotationGrace(RefreshToken refreshToken) {
        Instant rotatedAt = refreshToken.getRotatedAt();
        return rotatedAt != null && rotatedAt.plusMillis(rotationGraceMs).isAfter(Instant.now());
    }
}
