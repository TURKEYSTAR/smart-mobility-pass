package com.smartmobility.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ── Clé de signature ──────────────────────────────────────────────────────
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Vérifie la signature et l'expiration du token.
     * @return true si valide
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expiré : {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token JWT vide ou null : {}", e.getMessage());
        }
        return false;
    }

    // ── Extraction des claims ─────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extrait l'ID utilisateur (subject) */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Extrait l'email */
    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    /** Extrait le rôle : USER / MANAGER / ADMIN */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}
