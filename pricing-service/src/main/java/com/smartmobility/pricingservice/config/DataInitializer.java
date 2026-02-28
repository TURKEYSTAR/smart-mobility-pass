package com.smartmobility.pricingservice.config;

import com.smartmobility.pricingservice.entity.*;
import com.smartmobility.pricingservice.repository.DiscountPolicyRepository;
import com.smartmobility.pricingservice.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Initialise les données par défaut dans MySQL au démarrage
 * (Règles tarifaires + Politiques de réduction)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PricingRuleRepository pricingRuleRepository;
    private final DiscountPolicyRepository discountPolicyRepository;

    @Override
    public void run(String... args) {
        initPricingRules();
        initDiscountPolicies();
    }

    private void initPricingRules() {
        if (pricingRuleRepository.count() > 0) {
            log.info("[DataInit] Règles tarifaires déjà initialisées");
            return;
        }

        log.info("[DataInit] Initialisation des règles tarifaires...");

        // BUS CLASSIQUE
        pricingRuleRepository.save(PricingRule.builder()
                .transportType(TransportType.BUS_CLASSIQUE)
                .basePrice(BigDecimal.valueOf(150))
                .pricePerKm(BigDecimal.valueOf(25))
                .offPeakDiscount(BigDecimal.valueOf(20))
                .dailyCap(BigDecimal.valueOf(2000))
                .active(true)
                .validFrom(LocalDate.now())
                .build());

        // BRT
        pricingRuleRepository.save(PricingRule.builder()
                .transportType(TransportType.BRT)
                .basePrice(BigDecimal.valueOf(200))
                .pricePerKm(BigDecimal.valueOf(35))
                .offPeakDiscount(BigDecimal.valueOf(20))
                .dailyCap(BigDecimal.valueOf(2000))
                .active(true)
                .validFrom(LocalDate.now())
                .build());

        // TER
        pricingRuleRepository.save(PricingRule.builder()
                .transportType(TransportType.TER)
                .basePrice(BigDecimal.valueOf(300))
                .pricePerKm(BigDecimal.valueOf(50))
                .offPeakDiscount(BigDecimal.valueOf(20))
                .dailyCap(BigDecimal.valueOf(2000))
                .active(true)
                .validFrom(LocalDate.now())
                .build());

        log.info("[DataInit] ✅ 3 règles tarifaires créées (BUS_CLASSIQUE, BRT, TER)");
    }

    private void initDiscountPolicies() {
        if (discountPolicyRepository.count() > 0) {
            log.info("[DataInit] Politiques de réduction déjà initialisées");
            return;
        }

        log.info("[DataInit] Initialisation des politiques de réduction...");

        // Heures creuses (-20%)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Réduction Heures Creuses")
                .type(DiscountType.OFF_PEAK)
                .discountValue(BigDecimal.valueOf(20))
                .applicationTier(null)  // Pour tous les tiers
                .active(true)
                .build());

        // Tier SILVER (-10%)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Abonnement SILVER")
                .type(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .applicationTier("SILVER")
                .active(true)
                .build());

        // Tier GOLD (-15%)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Abonnement GOLD")
                .type(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(15))
                .applicationTier("GOLD")
                .active(true)
                .build());

        // Tier PLATINUM (-30%)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Abonnement PLATINUM")
                .type(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(30))
                .applicationTier("PLATINUM")
                .active(true)
                .build());

        // Fidélité (-5% après 10 trajets)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Programme Fidélité")
                .type(DiscountType.LOYALTY)
                .discountValue(BigDecimal.valueOf(5))
                .minTrips(10)
                .applicationTier(null)
                .active(true)
                .build());

        // Plafonnement journalier (2000 FCFA)
        discountPolicyRepository.save(DiscountPolicy.builder()
                .name("Plafonnement Journalier")
                .type(DiscountType.DAILY_CAP)
                .dailyCap(BigDecimal.valueOf(2000))
                .applicationTier(null)
                .active(true)
                .build());

        log.info("[DataInit] ✅ 6 politiques de réduction créées");
    }
}
