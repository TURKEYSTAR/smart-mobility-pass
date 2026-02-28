package com.smartmobility.pricingservice.controller;

import com.smartmobility.pricingservice.dto.FareResult;
import com.smartmobility.pricingservice.dto.PricingRequest;
import com.smartmobility.pricingservice.entity.FareCalculation;
import com.smartmobility.pricingservice.entity.PricingRule;
import com.smartmobility.pricingservice.service.FareCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * PricingController - API REST du Pricing & Discount Service
 *
 * Routes:
 * POST /pricing/calculate              → Calcule le tarif d'un trajet
 * GET  /pricing/rules                  → Liste toutes les règles tarifaires
 * GET  /pricing/calculation/{tripId}   → Calcul tarifaire d'un trajet spécifique
 * GET  /pricing/health                 → Health check
 */
@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final FareCalculatorService fareCalculatorService;

    /**
     * POST /pricing/calculate
     * Calcule le tarif complet avec toutes les règles métier
     * Appelé par le Trip Management Service
     */
    @PostMapping("/calculate")
    public ResponseEntity<FareResult> calculateFare(@Valid @RequestBody PricingRequest request) {
        log.info("[PricingController] POST /pricing/calculate - TripId={}, Type={}, Distance={}km",
                request.getTripId(), request.getTransportType(), request.getDistanceKm());

        FareResult result = fareCalculatorService.calculateFare(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /pricing/rules
     * Retourne toutes les règles tarifaires actives
     * Utilisé pour la consultation et la documentation
     */
    @GetMapping("/rules")
    public ResponseEntity<List<PricingRule>> getAllRules() {
        log.info("[PricingController] GET /pricing/rules");
        return ResponseEntity.ok(fareCalculatorService.getAllRules());
    }

    /**
     * GET /pricing/calculation/{tripId}
     * Récupère le détail d'un calcul tarifaire par tripId
     */
    @GetMapping("/calculation/{tripId}")
    public ResponseEntity<FareCalculation> getCalculationByTrip(@PathVariable UUID tripId) {
        log.info("[PricingController] GET /pricing/calculation/{}", tripId);
        FareCalculation calc = fareCalculatorService.getCalculationByTripId(tripId);
        return calc != null
                ? ResponseEntity.ok(calc)
                : ResponseEntity.notFound().build();
    }

    /**
     * GET /pricing/health
     * Endpoint de santé - également utilisé par Resilience4J pour le test HALF-OPEN
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pricing & Discount Service ✅ opérationnel - Port 8083");
    }
}
