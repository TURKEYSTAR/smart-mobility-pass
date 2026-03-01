package com.smartmobility.billingservice.client;

import com.smartmobility.billingservice.dto.PassResponse;
import com.smartmobility.billingservice.dto.UpdateSoldeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * UserServiceClient — billing-service → user-service
 *
 * ⚠️ On retourne PassResponse directement (pas ApiResponse<PassResponse>)
 *    car Feign ne gère pas bien les génériques à cause de l'effacement de type Java.
 *    Le user-service retourne bien { data: {...} } mais on mappe uniquement
 *    sur PassResponse via un décodeur personnalisé — ou on utilise Map pour extraire data.
 */
@FeignClient(name = "user-service", configuration = com.smartmobility.billingservice.config.FeignConfig.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}/pass")
    PassResponse getPass(
            @PathVariable("id") UUID userId,
            @RequestHeader("X-User-Id")   String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );

    @PutMapping("/api/users/{id}/pass/debiter")
    PassResponse debiterSolde(
            @PathVariable("id") UUID userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Id")   String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );

    @PutMapping("/api/users/{id}/pass/recharger")
    PassResponse rechargerSolde(
            @PathVariable("id") UUID userId,
            @RequestBody UpdateSoldeRequest request,
            @RequestHeader("X-User-Id")   String userIdHeader,
            @RequestHeader("X-User-Role") String role
    );
}