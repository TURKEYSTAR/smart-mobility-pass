package com.smartmobility.apigateway.filter;


import com.smartmobility.apigateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * Filtre JWT de l'API Gateway.
 *
 * Fonctionnement :
 *  1. Intercepte chaque requête entrante
 *  2. Extrait le token Bearer du header Authorization
 *  3. Valide la signature et l'expiration
 *  4. Injecte les infos utilisateur dans les headers → propagés aux microservices
 *  5. Bloque avec 401 si le token est absent/invalide
 *
 * Headers propagés aux microservices :
 *  - X-User-Id    → ID de l'utilisateur
 *  - X-User-Email → Email
 *  - X-User-Role  → Rôle (USER / MANAGER / ADMIN)
 *
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("JwtAuthFilter → requête : {}", path);

            // ── 1. Extraire le token du header Authorization ───────────────
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Token absent ou mal formaté pour : {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token manquant");
            }

            String token = authHeader.substring(7); // Enlève "Bearer "

            // ── 2. Valider le token ────────────────────────────────────────
            if (!jwtUtil.validateToken(token)) {
                log.warn("Token invalide ou expiré pour : {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token invalide");
            }

            // ── 3. Extraire les claims ─────────────────────────────────────
            Claims claims  = jwtUtil.extractAllClaims(token);
            String userId  = claims.getSubject();
            String email   = claims.get("email", String.class);
            String role    = claims.get("role", String.class);

            log.debug("Token valide → userId={}, role={}", userId, role);

            // ── 4. Injecter les infos dans les headers de la requête ───────
            //    Les microservices en aval lisent ces headers directement,
            //    sans avoir besoin de Spring Security ni de la clé JWT.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    userId)
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Role",  role  != null ? role  : "")
                    .build();

            // ── 5. Continuer la chaîne avec la requête enrichie ───────────
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    // ── Réponse d'erreur ──────────────────────────────────────────────────────
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes());
        return response.writeWith(Mono.just(body));
    }

    // ── Classe de configuration du filtre (peut être étendue) ────────────────
    public static class Config {
        // Paramètres optionnels configurables depuis application.yml
        // Ex : roles requis, whitelist de paths, etc.
    }
}