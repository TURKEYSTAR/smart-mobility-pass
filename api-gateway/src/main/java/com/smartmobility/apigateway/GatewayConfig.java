package com.smartmobility.apigateway;

import com.smartmobility.apigateway.filter.JwtAuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public GatewayConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── AUTH SERVICE (public — pas de JWT requis) ─────────────────
                // /api/auth/login, /api/auth/register → auth-service:/api/auth/...
                // Pas de stripPrefix car auth-service écoute sur /api/auth/**
                .route(p -> p
                        .path("/api/auth/**")
                        .uri("lb://AUTH-SERVICE"))

                // ── USER SERVICE (protégé JWT) ─────────────────────────────────
                // /api/users/** → user-service:/api/users/**
                // /api/passes/** → user-service:/api/passes/**
                .route(p -> p
                        .path("/api/users/**", "/api/passes/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://USER-SERVICE"))

                // ── TRIP SERVICE (protégé JWT) ─────────────────────────────────
                // /api/trips/** → trip-service:/trips/**
                .route(p -> p
                        .path("/api/trips/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://TRIP-SERVICE"))

                // ── PRICING SERVICE (protégé JWT) ──────────────────────────────
                // /api/pricing/** → pricing-service:/pricing/**
                .route(p -> p
                        .path("/api/pricing/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://PRICING-SERVICE"))

                // ── BILLING SERVICE (protégé JWT) ──────────────────────────────
                // /api/billing/** → billing-service:/api/billing/**
                // Pas de stripPrefix car billing écoute sur /api/billing/**
                .route(p -> p
                        .path("/api/billing/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://BILLING-SERVICE"))

                // ── NOTIFICATION SERVICE (protégé JWT) ────────────────────────
                // /api/notifications/** → notification-service:/notifications/**
                .route(p -> p
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .build();
    }
}