package com.smartmobility.pricingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client Feign vers le Billing Service
 * Utilisé pour vérifier le total journalier (plafonnement)
 */
@FeignClient(name = "billing-service", path = "/billing")
public interface BillingServiceClient {

    /**
     * Récupère le total des dépenses du jour pour un pass
     * GET /billing/daily-total/{passId}
     * Utilisé pour vérifier si le plafond journalier (2000 FCFA) est atteint
     */
    @GetMapping("/daily-total/{passId}")
    BigDecimal getDailyTotal(@PathVariable("passId") UUID passId);
}
