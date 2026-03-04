package com.smartmobility.pricingservice.controller;

import com.smartmobility.pricingservice.dto.FareResult;
import com.smartmobility.pricingservice.dto.PricingRequest;
import com.smartmobility.pricingservice.entity.FareCalculation;
import com.smartmobility.pricingservice.entity.PricingRule;
import com.smartmobility.pricingservice.entity.TransportType;
import com.smartmobility.pricingservice.lignes.Arret;
import com.smartmobility.pricingservice.lignes.Ligne;
import com.smartmobility.pricingservice.lignes.LigneRepository;
import com.smartmobility.pricingservice.lignes.TarifInfo;
import com.smartmobility.pricingservice.lignes.ZoneTarifService;
import com.smartmobility.pricingservice.service.FareCalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final FareCalculatorService fareCalculatorService;
    private final LigneRepository ligneRepository;
    private final ZoneTarifService zoneTarifService;

    /**
     * POST /pricing/calculate
     * Calcule le tarif par zones (appelé par Trip Service)
     */
    @PostMapping("/calculate")
    public ResponseEntity<FareResult> calculateFare(@Valid @RequestBody PricingRequest request) {
        log.info("[PricingController] POST /pricing/calculate - TripId={}, Type={}, Ligne={}",
                request.getTripId(), request.getTransportType(), request.getLigneId());
        return ResponseEntity.ok(fareCalculatorService.calculateFare(request));
    }

    /**
     * GET /pricing/lignes/{transportType}
     * Retourne toutes les lignes d'un type de transport.
     * Appelé par le frontend pour peupler la liste déroulante des lignes.
     * Ex: GET /pricing/lignes/BRT → [B1, B2, B3]
     */
    @GetMapping("/lignes/{transportType}")
    public ResponseEntity<List<Ligne>> getLignes(@PathVariable String transportType) {
        log.info("[PricingController] GET /pricing/lignes/{}", transportType);
        TransportType type = TransportType.valueOf(transportType.toUpperCase());
        return ResponseEntity.ok(ligneRepository.getLignesByType(type));
    }

    /**
     * GET /pricing/lignes/{ligneId}/arrets
     * Retourne tous les arrêts d'une ligne.
     * Appelé par le frontend après sélection de la ligne.
     * Ex: GET /pricing/lignes/BRT_B1/arrets → [Petersen, Nation, ...]
     */
    @GetMapping("/lignes/{ligneId}/arrets")
    public ResponseEntity<List<Arret>> getArrets(@PathVariable String ligneId) {
        log.info("[PricingController] GET /pricing/lignes/{}/arrets", ligneId);
        return ligneRepository.getLigneById(ligneId)
                .map(l -> ResponseEntity.ok(l.getArrets()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /pricing/tarif?transportType=BRT&ligneId=BRT_B1&arretDepartId=BRT_PETERSEN&arretArriveeId=BRT_PARCELLES
     * Retourne le tarif estimé pour un trajet donné (sans créer de calcul).
     * Utilisé par le frontend pour afficher le prix avant de démarrer.
     */
    @GetMapping("/tarif")
    public ResponseEntity<TarifInfo> getTarif(
            @RequestParam String transportType,
            @RequestParam String ligneId,
            @RequestParam String arretDepartId,
            @RequestParam String arretArriveeId) {

        log.info("[PricingController] GET /pricing/tarif - {} | {} | {} → {}",
                transportType, ligneId, arretDepartId, arretArriveeId);

        TransportType type = TransportType.valueOf(transportType.toUpperCase());
        TarifInfo info = zoneTarifService.getTarifInfo(type, ligneId, arretDepartId, arretArriveeId);
        return ResponseEntity.ok(info);
    }

    /**
     * GET /pricing/rules
     */
    @GetMapping("/rules")
    public ResponseEntity<List<PricingRule>> getAllRules() {
        return ResponseEntity.ok(fareCalculatorService.getAllRules());
    }

    /**
     * GET /pricing/calculation/{tripId}
     */
    @GetMapping("/calculation/{tripId}")
    public ResponseEntity<FareCalculation> getCalculationByTrip(@PathVariable UUID tripId) {
        FareCalculation calc = fareCalculatorService.getCalculationByTripId(tripId);
        return calc != null ? ResponseEntity.ok(calc) : ResponseEntity.notFound().build();
    }

    /**
     * GET /pricing/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pricing Service ✅ opérationnel — tarification par zones");
    }
}