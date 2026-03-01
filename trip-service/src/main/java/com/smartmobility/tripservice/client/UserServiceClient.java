package com.smartmobility.tripservice.client;

import com.smartmobility.tripservice.dto.PassValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * UserServiceClient — trip-service → user-service
 *
 * user-service expose GET /api/users/{id}/pass
 * On récupère le pass par userId (pas passId).
 * Le trip-service envoie X-User-Role: ADMIN pour accès interne.
 */
@FeignClient(name = "user-service", configuration = com.smartmobility.tripservice.config.FeignConfig.class)
public interface UserServiceClient {

    /**
     * Récupère et valide le pass d'un utilisateur via son userId.
     * GET /api/users/{id}/pass
     */
    @GetMapping("/api/users/{id}/pass")
    PassValidationResponse getPassByUserId(
            @PathVariable("id") UUID userId,
            @RequestHeader("X-User-Id")   String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );
}