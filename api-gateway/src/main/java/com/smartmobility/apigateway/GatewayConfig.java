package com.smartmobility.apigateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRouter(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                        .path("/api/users/**", "/api/passes/**")
                        .uri("lb://USER-SERVICE"))

                // nouvelles routes
                .route(p -> p.path("/api/trips/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://TRIP-SERVICE"))
                .route(p -> p.path("/api/pricing/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PRICING-SERVICE"))

                .route(p -> p
                        .path("/api/billing/**")
                        .uri("lb://BILLING-SERVICE"))

                .route(p -> p.path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://NOTIFICATION-SERVICE"))

                .build();
    }
}