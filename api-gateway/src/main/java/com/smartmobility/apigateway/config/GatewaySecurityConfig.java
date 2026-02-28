package com.smartmobility.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration Spring Security pour l'API Gateway.
 *
 * ⚠️  La Gateway utilise WebFlux (réactif), PAS Spring MVC.
 *     On utilise donc :
 *       - @EnableWebFluxSecurity (et non @EnableWebSecurity)
 *       - ServerHttpSecurity    (et non HttpSecurity)
 *       - SecurityWebFilterChain (et non SecurityFilterChain)
 *
 * Le vrai travail de sécurité est fait par JwtAuthFilter.
 * Ce fichier gère uniquement les routes publiques / protégées au niveau Gateway.
 *
 * Routes publiques (pas de JWT requis) :
 *   - POST /api/auth/login      → login
 *   - POST /api/auth/register   → inscription
 *   - GET  /actuator/health     → health check
 *   - GET  /v3/api-docs/**      → Swagger
 *
 * Toutes les autres routes passent par JwtAuthFilter.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // Désactivé : la Gateway est stateless (JWT), pas de session
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Autorisation des routes
                .authorizeExchange(auth -> auth
                        // Routes publiques — pas besoin de JWT
                        .pathMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()

                        // Tout le reste → doit avoir un JWT valide (vérifié par JwtAuthFilter)
                        .anyExchange().permitAll()
                );

        return http.build();
    }
}