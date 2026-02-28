package com.smartmobility.authservice.config;

import com.smartmobility.authservice.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig de l'auth-service.
 *
 * Ce service gère :
 *  - Login classique (email/password) → via AuthController
 *  - OAuth2 Google                    → via Spring Security + OAuth2SuccessHandler
 *
 * Il est STATELESS : pas de session HTTP, tout passe par JWT.
 *
 * Les endpoints /api/auth/** sont publics (pas de JWT requis).
 * Le JWT est généré ICI et validé par la Gateway pour les autres services.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
/*
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }
*/
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Stateless : pas de session ────────────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ── CSRF désactivé (API REST) ──────────────────────────────────
                .csrf(csrf -> csrf.disable())

                // ── Autorisations ──────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Tout /api/auth/** est public : login, register, oauth2 callback
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Tout le reste → authentifié (protégé par JWT via Gateway)
                        .anyRequest().authenticated()
                );
/*
                // ── OAuth2 Google ──────────────────────────────────────────────
                .oauth2Login(oauth -> oauth
                        // Point d'entrée : GET /oauth2/authorization/google
                        // Spring génère ce endpoint automatiquement
                        .successHandler(oAuth2SuccessHandler)
                        // En cas d'échec Google
                        .failureUrl("/api/auth/oauth2/failure")
                );
*/
        return http.build();
    }

    // ── PasswordEncoder ───────────────────────────────────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}