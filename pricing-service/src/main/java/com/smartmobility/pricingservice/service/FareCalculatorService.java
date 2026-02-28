package com.smartmobility.pricingservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmobility.pricingservice.client.BillingServiceClient;
import com.smartmobility.pricingservice.dto.FareResult;
import com.smartmobility.pricingservice.dto.PricingRequest;
import com.smartmobility.pricingservice.entity.*;
import com.smartmobility.pricingservice.repository.DiscountPolicyRepository;
import com.smartmobility.pricingservice.repository.FareCalculationRepository;
import com.smartmobility.pricingservice.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FareCalculatorService {

    private final PricingRuleRepository pricingRuleRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final FareCalculationRepository fareCalculationRepository;
    private final BillingServiceClient billingServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${pricing.daily-cap:2000}")
    private BigDecimal dailyCap;

    @Value("${pricing.off-peak.start-hour:22}")
    private int offPeakStartHour;

    @Value("${pricing.off-peak.end-hour:6}")
    private int offPeakEndHour;

    @Value("${pricing.discount.off-peak:20}")
    private BigDecimal offPeakDiscountPercent;

    @Value("${pricing.discount.loyalty.trips-required:10}")
    private int loyaltyTripsRequired;

    @Value("${pricing.discount.loyalty.percentage:5}")
    private BigDecimal loyaltyDiscountPercent;

    // ================================================================
    // CALCUL PRINCIPAL
    // ================================================================

    @Transactional
    public FareResult calculateFare(PricingRequest request) {
        log.info("[apigateway] ====== CALCUL TARIFAIRE ======");
        log.info("[apigateway] TripId={}, Type={}, Distance={}km, Tier={}, TotalTrajets={}",
                request.getTripId(), request.getTransportType(),
                request.getDistanceKm(), request.getPassTier(), request.getTotalTrips());

        // ---- ÉTAPE 1 : Récupération des règles tarifaires ----
        PricingRule rule = getPricingRule(request);

        // ---- ÉTAPE 2 : Calcul du prix de base ----
        BigDecimal baseAmount = calculateBasePrice(rule, request.getDistanceKm());
        log.info("[apigateway] Prix de base: {} FCFA ({} + {} × {}km)",
                baseAmount, rule.getBasePrice(), rule.getPricePerKm(), request.getDistanceKm());

        // ---- ÉTAPE 3 : Application des réductions ----
        List<String> appliedDiscounts = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal currentAmount = baseAmount;

        // Réduction heures creuses
        currentAmount = applyOffPeakDiscount(currentAmount, request.getDepartureTime(),
                appliedDiscounts);

        // Réduction par tier du pass
        currentAmount = applyTierDiscount(currentAmount, request.getPassTier(),
                appliedDiscounts);

        // Réduction fidélité
        currentAmount = applyLoyaltyDiscount(currentAmount, request.getTotalTrips(),
                appliedDiscounts);

        totalDiscount = baseAmount.subtract(currentAmount);

        // ---- ÉTAPE 4 : Vérification du plafond journalier ----
        boolean capped = false;
        BigDecimal finalAmount = currentAmount;

        try {
            BigDecimal dailyTotal = billingServiceClient.getDailyTotal(request.getPassId());
            log.info("[apigateway] Total journalier actuel: {} FCFA (plafond: {} FCFA)",
                    dailyTotal, dailyCap);

            if (dailyTotal.add(currentAmount).compareTo(dailyCap) > 0) {
                BigDecimal remaining = dailyCap.subtract(dailyTotal);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    finalAmount = remaining;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER appliqué (max " + dailyCap + " FCFA/jour)");
                    log.info("[apigateway] Plafond journalier atteint! Montant plafonné: {} FCFA → {} FCFA",
                            currentAmount, finalAmount);
                } else {
                    // Plafond déjà atteint → trajet gratuit
                    finalAmount = BigDecimal.ZERO;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER atteint - trajet sans frais");
                    log.info("[apigateway] Plafond journalier déjà atteint. Trajet gratuit!");
                }
            }
        } catch (Exception e) {
            log.warn("[apigateway] Impossible de vérifier le plafond journalier: {}", e.getMessage());
            // En cas d'erreur, on continue sans vérification du plafond
        }

        // S'assurer que le montant ne peut pas être négatif
        finalAmount = finalAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        totalDiscount = baseAmount.subtract(finalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // ---- ÉTAPE 5 : Sauvegarde du calcul ----
        saveCalculation(request, baseAmount, totalDiscount, finalAmount, appliedDiscounts, capped);

        log.info("[apigateway] === RÉSULTAT FINAL ===");
        log.info("[apigateway] Base: {} | Réduction: {} | Final: {} FCFA",
                baseAmount, totalDiscount, finalAmount);
        log.info("[apigateway] Réductions appliquées: {}", appliedDiscounts);

        return FareResult.builder()
                .baseAmount(baseAmount)
                .discountAmount(totalDiscount)
                .finalAmount(finalAmount)
                .appliedDiscounts(appliedDiscounts)
                .cappedByDailyLimit(capped)
                .fallbackUsed(false)
                .note(buildNote(appliedDiscounts, capped))
                .build();
    }

    // ================================================================
    // ÉTAPE 1 : Règles tarifaires
    // ================================================================

    private PricingRule getPricingRule(PricingRequest request) {
        return pricingRuleRepository
                .findByTransportTypeAndActiveTrue(request.getTransportType())
                .orElseGet(() -> {
                    log.warn("[apigateway] Règle non trouvée en DB pour {}, utilisation valeurs par défaut",
                            request.getTransportType());
                    return getDefaultRule(request.getTransportType());
                });
    }

    private PricingRule getDefaultRule(TransportType type) {
        return switch (type) {
            case BUS_CLASSIQUE -> PricingRule.builder()
                    .transportType(type).basePrice(BigDecimal.valueOf(150))
                    .pricePerKm(BigDecimal.valueOf(25)).build();
            case BRT -> PricingRule.builder()
                    .transportType(type).basePrice(BigDecimal.valueOf(200))
                    .pricePerKm(BigDecimal.valueOf(35)).build();
            case TER -> PricingRule.builder()
                    .transportType(type).basePrice(BigDecimal.valueOf(300))
                    .pricePerKm(BigDecimal.valueOf(50)).build();
        };
    }

    // ================================================================
    // ÉTAPE 2 : Calcul du prix de base
    // Formule: base = basePrice + (distance × pricePerKm)
    // ================================================================

    private BigDecimal calculateBasePrice(PricingRule rule, Double distanceKm) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal distanceCost = rule.getPricePerKm().multiply(distance);
        return rule.getBasePrice().add(distanceCost).setScale(2, RoundingMode.HALF_UP);
    }

    // ================================================================
    // ÉTAPE 3a : Réduction Heures Creuses (22h - 6h) → -20%
    // ================================================================

    private BigDecimal applyOffPeakDiscount(BigDecimal amount, LocalDateTime departureTime,
                                            List<String> appliedDiscounts) {
        if (departureTime == null) return amount;

        int hour = departureTime.getHour();
        boolean isOffPeak = hour >= offPeakStartHour || hour < offPeakEndHour;

        if (isOffPeak) {
            BigDecimal discount = amount.multiply(offPeakDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal reduced = amount.subtract(discount);
            appliedDiscounts.add("HEURES CREUSES (" + hour + "h00) -" + offPeakDiscountPercent + "%");
            log.info("[apigateway] Réduction heures creuses: -{} FCFA (-{}%)",
                    discount, offPeakDiscountPercent);
            return reduced;
        }
        return amount;
    }

    // ================================================================
    // ÉTAPE 3b : Réduction par Tier du Pass
    // SILVER: -10%, GOLD: -15%, PLATINUM: -30%
    // ================================================================

    private BigDecimal applyTierDiscount(BigDecimal amount, String passTier,
                                         List<String> appliedDiscounts) {
        if (passTier == null || passTier.equals("STANDARD")) return amount;

        BigDecimal tierDiscountPercent = switch (passTier.toUpperCase()) {
            case "SILVER"   -> BigDecimal.valueOf(10);
            case "GOLD"     -> BigDecimal.valueOf(15);
            case "PLATINUM" -> BigDecimal.valueOf(30);
            default         -> BigDecimal.ZERO;
        };

        if (tierDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = amount.multiply(tierDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal reduced = amount.subtract(discount);
            appliedDiscounts.add("TIER " + passTier.toUpperCase() + " -" + tierDiscountPercent + "%");
            log.info("[apigateway] Réduction tier {}: -{} FCFA (-{}%)",
                    passTier, discount, tierDiscountPercent);
            return reduced;
        }
        return amount;
    }

    // ================================================================
    // ÉTAPE 3c : Réduction Fidélité (>= 10 trajets) → -5%
    // ================================================================

    private BigDecimal applyLoyaltyDiscount(BigDecimal amount, int totalTrips,
                                            List<String> appliedDiscounts) {
        if (totalTrips >= loyaltyTripsRequired) {
            BigDecimal discount = amount.multiply(loyaltyDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal reduced = amount.subtract(discount);
            appliedDiscounts.add("FIDÉLITÉ (" + totalTrips + " trajets) -" + loyaltyDiscountPercent + "%");
            log.info("[apigateway] Réduction fidélité: -{} FCFA (-{}%)",
                    discount, loyaltyDiscountPercent);
            return reduced;
        }
        return amount;
    }

    // ================================================================
    // Sauvegarde du calcul en base
    // ================================================================

    private void saveCalculation(PricingRequest request, BigDecimal base,
                                 BigDecimal discount, BigDecimal finalAmount,
                                 List<String> discounts, boolean capped) {
        try {
            String discountsJson = objectMapper.writeValueAsString(discounts);
            FareCalculation calc = FareCalculation.builder()
                    .tripId(request.getTripId())
                    .passId(request.getPassId())
                    .baseAmount(base)
                    .discountAmount(discount)
                    .finalAmount(finalAmount)
                    .appliedDiscounts(discountsJson)
                    .cappedByDailyLimit(capped)
                    .fallbackUsed(false)
                    .build();
            fareCalculationRepository.save(calc);
            log.info("[apigateway] Calcul sauvegardé en DB ✅");
        } catch (Exception e) {
            log.error("[apigateway] Erreur sauvegarde calcul: {}", e.getMessage());
        }
    }

    private String buildNote(List<String> discounts, boolean capped) {
        if (discounts.isEmpty()) return "Tarif standard appliqué (aucune réduction)";
        StringBuilder note = new StringBuilder("Réductions appliquées: ");
        note.append(String.join(", ", discounts));
        if (capped) note.append(" | Plafond journalier atteint");
        return note.toString();
    }

    // ================================================================
    // LECTURE : Règles tarifaires
    // ================================================================

    public List<PricingRule> getAllRules() {
        return pricingRuleRepository.findAll();
    }

    public FareCalculation getCalculationByTripId(java.util.UUID tripId) {
        return fareCalculationRepository.findByTripId(tripId).orElse(null);
    }
}