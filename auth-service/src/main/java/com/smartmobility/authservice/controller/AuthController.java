package com.smartmobility.authservice.controller;

import com.smartmobility.authservice.dto.AuthResponse;
import com.smartmobility.authservice.dto.LoginRequest;
import com.smartmobility.authservice.dto.RegisterRequest;
import com.smartmobility.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — endpoints publics d'authentification.
 *
 * Tous ces endpoints sont accessibles SANS JWT (routes publiques dans la Gateway).
 *
 * POST /api/auth/register        → inscription classique
 * POST /api/auth/login           → login email/password
 * GET  /api/auth/oauth2/callback → appelé automatiquement par Spring après Google
 * GET  /api/auth/me              → vérifie que le token est valide (optionnel)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Register ──────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── OAuth2 Google — callback ──────────────────────────────────────────────
    /**
     * Spring Security redirige ici après une authentification Google réussie.
     * @AuthenticationPrincipal OAuth2User → profil Google injecté automatiquement.
     *
     * URL configurée dans application.properties :
     *   spring.security.oauth2.client.registration.google.redirect-uri=
     *     {baseUrl}/api/auth/oauth2/callback/google
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> oauth2Callback(
            @AuthenticationPrincipal OAuth2User oAuth2User) {

        if (oAuth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentification OAuth2 échouée"));
        }

        String email    = oAuth2User.getAttribute("email");
        String nom      = oAuth2User.getAttribute("family_name");
        String prenom   = oAuth2User.getAttribute("given_name");
        String googleId = oAuth2User.getAttribute("sub");

        AuthResponse response = authService.loginOAuth2(email, nom, prenom, googleId);
        return ResponseEntity.ok(response);
    }

    // ── Vérification token (optionnel, pratique pour les tests) ───────────────
    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        // Si on arrive ici, le JWT est valide (vérifié par la Gateway)
        // Utile pour tester que la chaîne Gateway → auth-service fonctionne
        return ResponseEntity.ok(Map.of("status", "Token valide"));
    }
}
