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
 * Ce service NE touche PAS à la base de données.
 * Il délègue toute la persistance au user-service via Feign.
 *
 * JWT stateless : généré ici, validé par la Gateway, jamais stocké en BDD.
 *
 * Responsabilités :
 *  - register    → encode le mot de passe, crée le user via Feign, génère JWT
 *  - login       → vérifie le mot de passe, génère JWT
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

        // 2. Créer le user dans le user-service via Feign
        // Ordre record CreateUserRequest : (nom, prenom, email, password, username, telephone, role, enabled, googleId)
        CreateUserRequest createRequest = new CreateUserRequest(
                request.nom(),
                request.prenom(),
                request.email(),
                passwordEncoder.encode(request.password()),  // encodage BCrypt ici
                request.username(),
                request.telephone(),
                "USER",   // rôle par défaut
                true,     // compte activé
                null      // pas de googleId
        );

        UserDto created = userServiceClient.createUser(createRequest);
        log.info("Nouveau compte créé : {}", created.email());

        // 3. Générer le JWT stateless (non stocké)
        String token = jwtUtil.generateToken(created.id(), created.email(), created.role());
        return new AuthResponse(
                token,
                created.id(),
                created.email(),
                created.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime()
        );
    }

    // ── Login classique ───────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {

        // 1. Chercher l'utilisateur par email
        UserDto user = findUserByEmailSafe(request.email());
        if (user == null) {
            // Message volontairement vague pour ne pas révéler si l'email existe
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        // 2. Vérifier le mot de passe contre le hash BCrypt
        if (!passwordEncoder.matches(request.password(), user.password())) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        // 3. Vérifier que le compte est actif
        if (!user.enabled()) {
            throw new IllegalStateException("Compte désactivé");
        }

        log.info("Login réussi : {}", user.email());

        // 4. Générer le JWT stateless
        String token = jwtUtil.generateToken(user.id(), user.email(), user.role());
        return new AuthResponse(
                token,
                user.id(),
                user.email(),
                user.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime()
        );
    }

    // ── Login OAuth2 Google ───────────────────────────────────────────────────

    /**
     * Appelé par OAuth2SuccessHandler après authentification Google réussie.
     * Crée le compte automatiquement à la première connexion.
     */
    public AuthResponse loginOAuth2(String email, String nom, String prenom, String googleId) {

        UserDto user = findUserByEmailSafe(email);

        if (user == null) {
            log.info("Première connexion OAuth2 Google, création du compte : {}", email);

            // Ordre record CreateUserRequest : (nom, prenom, email, password, username, telephone, role, enabled, googleId)
            CreateUserRequest createRequest = new CreateUserRequest(
                    nom      != null ? nom      : "",   // nom
                    prenom   != null ? prenom   : "",   // prenom
                    email,                               // email
                    null,                                // password → null pour OAuth2 (pas de mot de passe)
                    email,                               // username = email par défaut
                    null,                                // telephone → inconnu via Google
                    "USER",                              // role par défaut
                    true,                                // enabled
                    googleId                             // googleId
            );
            user = userServiceClient.createUser(createRequest);
        }

        if (!user.enabled()) {
            throw new IllegalStateException("Compte désactivé");
        }

        String token = jwtUtil.generateToken(user.id(), user.email(), user.role());
        return new AuthResponse(
                token,
                user.id(),
                user.email(),
                user.role(),
                jwtUtil.extractAllClaims(token).getExpiration().getTime()
        );
    }

    // ── Utilitaire interne ────────────────────────────────────────────────────

    /**
     * Cherche un user par email sans lever d'exception si non trouvé (404).
     * Feign lève FeignException.NotFound pour un 404 → on retourne null proprement.
     */
    private UserDto findUserByEmailSafe(String email) {
        try {
            return userServiceClient.findByEmail(email);
        } catch (FeignException.NotFound e) {
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'utilisateur {} : {}", email, e.getMessage());
            throw new IllegalStateException("Service utilisateur indisponible", e);
        }
    }
}