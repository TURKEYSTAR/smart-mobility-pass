package com.smartmobility.userservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig user-service.
 *
 * Ce service ne fait PAS d'authentification — c'est le rôle de l'auth-service.
 * La Gateway vérifie le JWT et injecte X-User-Id / X-User-Role dans chaque requête.
 * Ici on fait confiance à ces headers, donc tout est permitAll.
 *
 * On garde Spring Security uniquement pour désactiver CSRF et forcer le mode stateless.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}