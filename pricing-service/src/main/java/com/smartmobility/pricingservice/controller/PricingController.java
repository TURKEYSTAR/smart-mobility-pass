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

    @PostMapping("/calculate")
    public ResponseEntity<FareResult> calculateFare(@Valid @RequestBody PricingRequest request) {
        log.info("[PricingController] totalTrips reçu={} | passTier={}",
                request.getTotalTrips(), request.getPassTier());
        log.info("[PricingController] POST /pricing/calculate - TripId={}, Type={}, Ligne={}",
                request.getTripId(), request.getTransportType(), request.getLigneId());
        return ResponseEntity.ok(fareCalculatorService.calculateFare(request));
    }

    @GetMapping("/lignes/{transportType}")
    public ResponseEntity<List<Ligne>> getLignes(@PathVariable String transportType) {
        TransportType type = TransportType.valueOf(transportType.toUpperCase());
        return ResponseEntity.ok(ligneRepository.getLignesByType(type));
    }

    @GetMapping("/lignes/{ligneId}/arrets")
    public ResponseEntity<List<Arret>> getArrets(@PathVariable String ligneId) {
        return ligneRepository.getLigneById(ligneId)
                .map(l -> ResponseEntity.ok(l.getArrets()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /pricing/tarif
     * Devis détaillé avec réductions (heures creuses, tier, fidélité).
     * passTier et totalTrips sont optionnels (défaut : STANDARD, 0).
     */
    @GetMapping("/tarif")
    public ResponseEntity<TarifInfo> getTarif(
            @RequestParam String transportType,
            @RequestParam String ligneId,
            @RequestParam String arretDepartId,
            @RequestParam String arretArriveeId,
            @RequestParam(required = false, defaultValue = "STANDARD") String passTier,
            @RequestParam(required = false, defaultValue = "0") int totalTrips) {

        log.info("[PricingController] GET /pricing/tarif - {} | {} → {} | tier={} trips={}",
                transportType, arretDepartId, arretArriveeId, passTier, totalTrips);

        TransportType type = TransportType.valueOf(transportType.toUpperCase());
        TarifInfo info = zoneTarifService.getTarifInfo(
                type, ligneId, arretDepartId, arretArriveeId, passTier, totalTrips);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<PricingRule>> getAllRules() {
        return ResponseEntity.ok(fareCalculatorService.getAllRules());
    }

    @GetMapping("/calculation/{tripId}")
    public ResponseEntity<FareCalculation> getCalculationByTrip(@PathVariable UUID tripId) {
        FareCalculation calc = fareCalculatorService.getCalculationByTripId(tripId);
        return calc != null ? ResponseEntity.ok(calc) : ResponseEntity.notFound().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pricing Service opérationnel — tarification par zones");
    }
}