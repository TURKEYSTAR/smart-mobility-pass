package com.smartmobility.pricingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmobility.pricingservice.client.BillingClientWrapper;
import com.smartmobility.pricingservice.dto.FareResult;
import com.smartmobility.pricingservice.dto.PricingRequest;
import com.smartmobility.pricingservice.entity.FareCalculation;
import com.smartmobility.pricingservice.entity.PricingRule;
import com.smartmobility.pricingservice.entity.TransportType;
import com.smartmobility.pricingservice.lignes.ZoneTarifService;
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

    private final FareCalculationRepository fareCalculationRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final BillingClientWrapper billingClientWrapper;
    private final ZoneTarifService zoneTarifService;
    private final ObjectMapper objectMapper;

    @Value("${pricing.daily-cap:5000}")
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
        log.info("[FareCalculator] ====== CALCUL TARIFAIRE PAR ZONES ======");
        log.info("[FareCalculator] TripId={}, Type={}, Ligne={}, {} → {}",
                request.getTripId(), request.getTransportType(),
                request.getLigneId(), request.getArretDepartId(), request.getArretArriveeId());

        // ---- ÉTAPE 1 : Calcul du tarif de base par zones ----
        BigDecimal baseAmount = zoneTarifService.calculerTarif(
                request.getTransportType(),
                request.getLigneId(),
                request.getArretDepartId(),
                request.getArretArriveeId()
        );
        log.info("[FareCalculator] Tarif de base (par zones) : {} FCFA", baseAmount);

        // ---- ÉTAPE 2 : Application des réductions ----
        List<String> appliedDiscounts = new ArrayList<>();
        BigDecimal currentAmount = baseAmount;

        currentAmount = applyOffPeakDiscount(currentAmount, request.getDepartureTime(), appliedDiscounts);
        currentAmount = applyTierDiscount(currentAmount, request.getPassTier(), appliedDiscounts);
        currentAmount = applyLoyaltyDiscount(currentAmount, request.getTotalTrips(), appliedDiscounts);

        BigDecimal totalDiscount = baseAmount.subtract(currentAmount);

        // ---- ÉTAPE 3 : Vérification du plafond journalier ----
        boolean capped = false;
        BigDecimal finalAmount = currentAmount;

        BigDecimal dailyTotal = billingClientWrapper.getDailyTotal(request.getPassId());
        if (dailyTotal != null) {
            log.info("[FareCalculator] Total journalier actuel : {} FCFA (plafond : {} FCFA)",
                    dailyTotal, dailyCap);

            if (dailyTotal.add(currentAmount).compareTo(dailyCap) > 0) {
                BigDecimal remaining = dailyCap.subtract(dailyTotal);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    finalAmount = remaining;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER appliqué (max " + dailyCap + " FCFA/jour)");
                    log.info("[FareCalculator] Plafond atteint ! {} FCFA → {} FCFA", currentAmount, finalAmount);
                } else {
                    finalAmount = BigDecimal.ZERO;
                    capped = true;
                    appliedDiscounts.add("PLAFOND JOURNALIER atteint — trajet sans frais");
                }
            }
        } else {
            log.warn("[FareCalculator] Billing-service indisponible, plafond non vérifié");
        }

        finalAmount = finalAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        totalDiscount = baseAmount.subtract(finalAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        // ---- ÉTAPE 4 : Sauvegarde ----
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
    // RÉDUCTIONS
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
            log.info("[FareCalculator] Heures creuses : -{} FCFA", discount);
            return amount.subtract(discount);
        }
        return amount;
    }

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
            return amount.subtract(discount);
        }
        return amount;
    }

    private BigDecimal applyLoyaltyDiscount(BigDecimal amount, int totalTrips,
                                            List<String> appliedDiscounts) {
        if (totalTrips >= loyaltyTripsRequired) {
            BigDecimal discount = amount.multiply(loyaltyDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            appliedDiscounts.add("FIDÉLITÉ (" + totalTrips + " trajets) -" + loyaltyDiscountPercent + "%");
            return amount.subtract(discount);
        }
        return amount;
    }

    // ================================================================
    // SAUVEGARDE
    // ================================================================

    private void saveCalculation(PricingRequest request, BigDecimal base,
                                 BigDecimal discount, BigDecimal finalAmount,
                                 List<String> discounts, boolean capped) {
        try {
            String discountsJson = objectMapper.writeValueAsString(discounts);
            // On stocke la ligne + arrêts dans le champ note de FareCalculation
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
            log.error("[FareCalculator] Erreur sauvegarde : {}", e.getMessage());
        }
    }

    private String buildNote(List<String> discounts, boolean capped) {
        if (discounts.isEmpty()) return "Tarif par zone — aucune réduction";
        StringBuilder note = new StringBuilder("Réductions : ").append(String.join(", ", discounts));
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