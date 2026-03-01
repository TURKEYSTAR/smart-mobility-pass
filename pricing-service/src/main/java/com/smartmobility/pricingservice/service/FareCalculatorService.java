package com.smartmobility.pricingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmobility.pricingservice.client.BillingClientWrapper;
import com.smartmobility.pricingservice.dto.FareResult;
import com.smartmobility.pricingservice.dto.PricingRequest;
import com.smartmobility.pricingservice.entity.*;
import com.smartmobility.pricingservice.exception.PricingRuleNotFoundException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FareCalculatorService {

    private final PricingRuleRepository pricingRuleRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final FareCalculationRepository fareCalculationRepository;
    private final BillingClientWrapper billingClientWrapper;
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
        log.info("[FareCalculator] ====== CALCUL TARIFAIRE ======");
        log.info("[FareCalculator] TripId={}, Type={}, Distance={}km, Tier={}, TotalTrajets={}",
                request.getTripId(), request.getTransportType(),
                request.getDistanceKm(), request.getPassTier(), request.getTotalTrips());

        // ---- ÉTAPE 1 : Récupération des règles tarifaires ----
        PricingRule rule = getPricingRule(request);

        // ---- ÉTAPE 2 : Calcul du prix de base ----
        BigDecimal baseAmount = calculateBasePrice(rule, request.getDistanceKm());
        log.info("[FareCalculator] Prix de base: {} FCFA ({} + {} × {}km)",
                baseAmount, rule.getBasePrice(), rule.getPricePerKm(), request.getDistanceKm());

        // ---- ÉTAPE 3 : Application des réductions ----
        List<String> appliedDiscounts = new ArrayList<>();
        BigDecimal currentAmount = baseAmount;

        currentAmount = applyOffPeakDiscount(currentAmount, request.getDepartureTime(), appliedDiscounts);
        currentAmount = applyTierDiscount(currentAmount, request.getPassTier(), appliedDiscounts);
        currentAmount = applyLoyaltyDiscount(currentAmount, request.getTotalTrips(), appliedDiscounts);

        BigDecimal totalDiscount = baseAmount.subtract(currentAmount);

        // ---- ÉTAPE 4 : Vérification du plafond journalier (avec circuit breaker) ----
        boolean capped = false;
        BigDecimal finalAmount = currentAmount;

        BigDecimal dailyTotal = getDailyTotalSafe(request.getPassId());
        if (dailyTotal != null) {
            log.info("[FareCalculator] Total journalier actuel: {} FCFA (plafond: {} FCFA)",
                    dailyTotal, dailyCap);

            if (dailyTotal.add(currentAmount).compareTo(dailyCap) > 0) {
                BigDecimal remaining = dailyCap.subtract(dailyTotal);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    finalAmount = remaining;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER appliqué (max " + dailyCap + " FCFA/jour)");
                    log.info("[FareCalculator] Plafond atteint! {} FCFA → {} FCFA", currentAmount, finalAmount);
                } else {
                    finalAmount = BigDecimal.ZERO;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER atteint - trajet sans frais");
                    log.info("[FareCalculator] Plafond déjà atteint. Trajet gratuit!");
                }
            }
        } else {
            log.warn("[FareCalculator] Billing-service indisponible, plafond journalier non vérifié");
        }

        // Montant final jamais négatif
        finalAmount = finalAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        totalDiscount = baseAmount.subtract(finalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // ---- ÉTAPE 5 : Sauvegarde ----
        saveCalculation(request, baseAmount, totalDiscount, finalAmount, appliedDiscounts, capped);

        log.info("[FareCalculator] === RÉSULTAT === Base:{} | Réduction:{} | Final:{} FCFA",
                baseAmount, totalDiscount, finalAmount);

        return FareResult.builder()
                .baseAmount(baseAmount)
                .discountAmount(totalDiscount)
                .finalAmount(finalAmount)
                .appliedDiscounts(appliedDiscounts)
                .cappedByDailyLimit(capped)
                .fallbackUsed(dailyTotal == null)
                .note(buildNote(appliedDiscounts, capped))
                .build();
    }

    // ================================================================
    // Appel vers billing-service via wrapper (Circuit Breaker dans BillingClientWrapper)
    // ================================================================

    private BigDecimal getDailyTotalSafe(UUID passId) {
        return billingClientWrapper.getDailyTotal(passId);
    }

    // ================================================================
    // ÉTAPE 1 : Règles tarifaires
    // ================================================================

    private PricingRule getPricingRule(PricingRequest request) {
        return pricingRuleRepository
                .findByTransportTypeAndActiveTrue(request.getTransportType())
                .orElseGet(() -> {
                    log.warn("[FareCalculator] Règle non trouvée pour {}, valeurs par défaut",
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
    // ÉTAPE 2 : Prix de base = basePrice + (distance × pricePerKm)
    // ================================================================

    private BigDecimal calculateBasePrice(PricingRule rule, Double distanceKm) {
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        return rule.getBasePrice()
                .add(rule.getPricePerKm().multiply(distance))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ================================================================
    // ÉTAPE 3a : Heures creuses (22h–6h) → -20%
    // ================================================================

    private BigDecimal applyOffPeakDiscount(BigDecimal amount, LocalDateTime departureTime,
                                            List<String> appliedDiscounts) {
        if (departureTime == null) return amount;
        int hour = departureTime.getHour();
        boolean isOffPeak = hour >= offPeakStartHour || hour < offPeakEndHour;
        if (isOffPeak) {
            BigDecimal discount = amount.multiply(offPeakDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            appliedDiscounts.add("HEURES CREUSES (" + hour + "h) -" + offPeakDiscountPercent + "%");
            log.info("[FareCalculator] Heures creuses: -{} FCFA", discount);
            return amount.subtract(discount);
        }
        return amount;
    }

    // ================================================================
    // ÉTAPE 3b : Réduction tier (SILVER -10%, GOLD -15%, PLATINUM -30%)
    // ================================================================

    private BigDecimal applyTierDiscount(BigDecimal amount, String passTier,
                                         List<String> appliedDiscounts) {
        if (passTier == null || passTier.equalsIgnoreCase("STANDARD")) return amount;

        BigDecimal tierDiscountPercent = switch (passTier.toUpperCase()) {
            case "SILVER"   -> BigDecimal.valueOf(10);
            case "GOLD"     -> BigDecimal.valueOf(15);
            case "PLATINUM" -> BigDecimal.valueOf(30);
            default         -> BigDecimal.ZERO;
        };

        if (tierDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = amount.multiply(tierDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            appliedDiscounts.add("TIER " + passTier.toUpperCase() + " -" + tierDiscountPercent + "%");
            log.info("[FareCalculator] Tier {}: -{} FCFA", passTier, discount);
            return amount.subtract(discount);
        }
        return amount;
    }

    // ================================================================
    // ÉTAPE 3c : Fidélité (>= 10 trajets) → -5%
    // ================================================================

    private BigDecimal applyLoyaltyDiscount(BigDecimal amount, int totalTrips,
                                            List<String> appliedDiscounts) {
        if (totalTrips >= loyaltyTripsRequired) {
            BigDecimal discount = amount.multiply(loyaltyDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            appliedDiscounts.add("FIDÉLITÉ (" + totalTrips + " trajets) -" + loyaltyDiscountPercent + "%");
            log.info("[FareCalculator] Fidélité: -{} FCFA", discount);
            return amount.subtract(discount);
        }
        return amount;
    }

    // ================================================================
    // Sauvegarde historique
    // ================================================================

    private void saveCalculation(PricingRequest request, BigDecimal base,
                                 BigDecimal discount, BigDecimal finalAmount,
                                 List<String> discounts, boolean capped) {
        try {
            String discountsJson = objectMapper.writeValueAsString(discounts);
            fareCalculationRepository.save(FareCalculation.builder()
                    .tripId(request.getTripId())
                    .passId(request.getPassId())
                    .baseAmount(base)
                    .discountAmount(discount)
                    .finalAmount(finalAmount)
                    .appliedDiscounts(discountsJson)
                    .cappedByDailyLimit(capped)
                    .fallbackUsed(false)
                    .build());
            log.info("[FareCalculator] Calcul sauvegardé ✅");
        } catch (Exception e) {
            log.error("[FareCalculator] Erreur sauvegarde: {}", e.getMessage());
        }
    }

    private String buildNote(List<String> discounts, boolean capped) {
        if (discounts.isEmpty()) return "Tarif standard appliqué (aucune réduction)";
        StringBuilder note = new StringBuilder("Réductions: ").append(String.join(", ", discounts));
        if (capped) note.append(" | Plafond journalier atteint");
        return note.toString();
    }

    // ================================================================
    // READ
    // ================================================================

    public List<PricingRule> getAllRules() {
        return pricingRuleRepository.findAll();
    }

    public FareCalculation getCalculationByTripId(UUID tripId) {
        return fareCalculationRepository.findByTripId(tripId).orElse(null);
    }
}