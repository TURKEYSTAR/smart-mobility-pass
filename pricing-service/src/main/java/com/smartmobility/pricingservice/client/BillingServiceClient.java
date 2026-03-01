package com.smartmobility.pricingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client Feign vers le Billing Service.
 * Utilisé pour vérifier le total journalier (plafonnement à 2000 FCFA/jour).
 *
 * Le billing-service écoute sur /api/billing/**
 * La Gateway route /api/billing/** → billing-service sans stripPrefix
 * → Feign appelle directement billing-service via lb://
 */
@FeignClient(name = "billing-service", path = "/api/billing")
public interface BillingServiceClient {

    /**
     * GET /api/billing/daily-total/{passId}
     * Retourne le total des dépenses du jour pour un pass donné.
     * Utilisé pour le plafonnement journalier (2000 FCFA).
     */
    @GetMapping("/daily-total/{passId}")
    BigDecimal getDailyTotal(@PathVariable("passId") UUID passId);
}