package com.smartmobility.billingservice.client;

import com.smartmobility.billingservice.dto.ApiResponse;
import com.smartmobility.billingservice.dto.PassResponse;
import com.smartmobility.billingservice.dto.UpdateSoldeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * UserServiceClient — billing-service
 *
 * Appelle le user-service pour :
 *  - débiter le solde du pass après un trajet
 *  - recharger le solde du pass
 *
 * Le billing-service se comporte comme un MANAGER donc il envoie
 * X-User-Role: MANAGER dans les headers Feign pour passer le contrôle
 * d'accès du user-service.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Débite le solde du pass d'un utilisateur.
     * PUT /api/users/{userId}/pass/debiter
     */
    @PutMapping("/api/users/{userId}/pass/debiter")
    ApiResponse<PassResponse> debiterSolde(
            @PathVariable("userId") Long userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * Recharge le solde du pass d'un utilisateur.
     * PUT /api/users/{userId}/pass/recharger
     */
    @PutMapping("/api/users/{userId}/pass/recharger")
    ApiResponse<PassResponse> rechargerSolde(
            @PathVariable("userId") Long userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Id") String userId2,
            @RequestHeader("X-User-Role") String role
    );

    /**
     * Récupère le solde journalier d'un pass via passId.
     * Utilisé par le pricing-service pour le plafonnement journalier.
     * GET /api/users/{userId}/pass
     */
    @org.springframework.web.bind.annotation.GetMapping("/api/users/{userId}/pass")
    ApiResponse<PassResponse> getPass(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestHeader("X-User-Role") String role
    );
}
