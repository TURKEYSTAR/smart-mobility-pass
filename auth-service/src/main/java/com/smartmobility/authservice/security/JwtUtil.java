package com.smartmobility.authservice.security;

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
import java.util.Date;
import java.util.UUID;

/**
 * JwtUtil — auth-service
 *
 * Contrairement à la Gateway qui ne fait que VALIDER,
 * l'auth-service fait les deux : GÉNÉRER + VALIDER.
 *
 * API jjwt 0.12.x :
 *   Génération  → .subject() .issuedAt() .expiration()
 *   Validation  → .parser() .verifyWith() .parseSignedClaims() .getPayload()
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Génération ────────────────────────────────────────────────────────────

    /**
     * Génère un JWT signé avec les infos de l'utilisateur.
     * Structure du token :
     * {
     *   "sub":   "42",           ← userId
     *   "email": "user@mail.com",
     *   "role":  "USER",
     *   "iat":   ...,
     *   "exp":   ...
     * }
     */
    public String generateToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expiré : {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token invalide : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token vide : {}", e.getMessage());
        }
        return false;
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}