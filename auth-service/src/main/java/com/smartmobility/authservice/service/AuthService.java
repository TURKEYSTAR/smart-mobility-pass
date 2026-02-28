package com.smartmobility.authservice.service;

import com.smartmobility.authservice.client.UserServiceClient;
import com.smartmobility.authservice.dto.*;
import com.smartmobility.authservice.security.JwtUtil;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AuthService — logique métier de l'authentification.
 *
 * Ce service NE touche PAS à la base de données directement.
 * Il délègue toute la persistance au user-service via Feign.
 *
 * Responsabilités :
 *  - register   → encode le mot de passe, crée le user via Feign, génère JWT
 *  - login      → vérifie le mot de passe, génère JWT
 *  - loginOAuth2 → crée ou récupère le user Google, génère JWT
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserServiceClient userServiceClient,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest request) {

        // 1. Vérifier si l'email existe déjà
        UserDto existing = findUserByEmailSafe(request.email());
        if (existing != null) {
            throw new IllegalArgumentException("Email déjà utilisé : " + request.email());
        }

        // 2. Créer le user dans le user-service
        CreateUserRequest createRequest = new CreateUserRequest(
                 // hash BCrypt ici
                request.nom(),
                request.prenom(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.username(),
                request.telephone(),
                "USER",   // rôle par défaut
                true,
                null      // pas de googleId
        );

        UserDto created = userServiceClient.createUser(createRequest);
        log.info("Nouveau compte créé : {}", created.email());

        // 3. Générer et retourner le JWT
        String token = jwtUtil.generateToken(created.id(), created.email(), created.role());
        return new AuthResponse(token, created.id(), created.email(), created.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime());
    }

    // ── Login classique ───────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {

        // 1. Chercher l'utilisateur
        UserDto user = findUserByEmailSafe(request.email());

        if (user == null) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        // 3. Vérifier que le compte est actif
        if (!user.enabled()) {
            throw new IllegalStateException("Compte désactivé");
        }

        log.info("Login réussi : {}", user.email());

        // 4. Générer le JWT
        String token = jwtUtil.generateToken(user.id(), user.email(), user.role());
        return new AuthResponse(token, user.id(), user.email(), user.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime());
    }

    // ── Login OAuth2 Google ───────────────────────────────────────────────────

    /**
     * Appelé par OAuth2SuccessHandler après authentification Google réussie.
     * Crée le compte si première connexion, sinon met à jour le googleId.
     */
    public AuthResponse loginOAuth2(String email, String nom, String prenom, String googleId) {

        UserDto user = findUserByEmailSafe(email);

        if (user == null) {
            // Première connexion Google → créer le compte automatiquement
            log.info("Première connexion OAuth2, création du compte : {}", email);

            CreateUserRequest createRequest = new CreateUserRequest(
                    email,
                    null,      // pas de mot de passe pour OAuth2
                    nom != null ? nom : "",
                    prenom != null ? prenom : "",
                    email,     // username = email par défaut
                    null,      // pas de téléphone
                    "USER",
                    true,
                    googleId
            );
            user = userServiceClient.createUser(createRequest);
        }

        if (!user.enabled()) {
            throw new IllegalStateException("Compte désactivé");
        }

        String token = jwtUtil.generateToken(user.id(), user.email(), user.role());
        return new AuthResponse(token, user.id(), user.email(), user.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime());
    }

    // ── Utilitaire interne ────────────────────────────────────────────────────

    /**
     * Cherche un user par email sans lever d'exception si non trouvé (404).
     * Feign lève FeignException.NotFound → on retourne null.
     */
    private UserDto findUserByEmailSafe(String email) {
        try {
            return userServiceClient.findByEmail(email);
        } catch (FeignException.NotFound e) {
            return null;
        }
    }
}