package com.smartmobility.billingservice.client;

import com.smartmobility.billingservice.dto.ApiResponse;
import com.smartmobility.billingservice.dto.PassResponse;
import com.smartmobility.billingservice.dto.UpdateSoldeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * UserServiceClient — billing-service → user-service
 *
 * Le billing-service est un service INTERNE.
 * Il se comporte comme un MANAGER : il envoie lui-même
 * X-User-Role: MANAGER dans chaque appel Feign.
 *
 * Règle simple :
 *   - debiterSolde  → réservé MANAGER/ADMIN dans PassMobilityController
 *                     → on envoie juste X-User-Role
 *   - rechargerSolde → accessible au propriétaire OU MANAGER/ADMIN
 *                      → on envoie X-User-Id (l'userId cible) + X-User-Role
 *   - getPass        → accessible au propriétaire OU MANAGER/ADMIN
 *                      → on envoie X-User-Id + X-User-Role
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * PUT /api/users/{userId}/pass/debiter
     *
     * PassMobilityController.debiterSolde() attend :
     *   @RequestHeader("X-User-Role") String userRole
     * → seul MANAGER/ADMIN autorisé → on envoie "MANAGER"
     */
    @PutMapping("/api/users/{userId}/pass/debiter")
    ApiResponse<PassResponse> debiterSolde(
            @PathVariable("userId") UUID userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * PUT /api/users/{userId}/pass/recharger
     *
     * PassMobilityController.rechargerSolde() attend :
     *   @RequestHeader("X-User-Id")   String userId   ← l'userId cible
     *   @RequestHeader("X-User-Role") String userRole
     * → propriétaire OU MANAGER/ADMIN autorisé
     * → on envoie l'userId cible + "MANAGER"
     */
    @PutMapping("/api/users/{userId}/pass/recharger")
    ApiResponse<PassResponse> rechargerSolde(
            @PathVariable("userId") UUID userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * GET /api/users/{userId}/pass
     *
     * PassMobilityController.obtenirPass() attend :
     *   @RequestHeader("X-User-Id")   String userId
     *   @RequestHeader("X-User-Role") String userRole
     */
    @GetMapping("/api/users/{userId}/pass")
    ApiResponse<PassResponse> getPass(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );
}