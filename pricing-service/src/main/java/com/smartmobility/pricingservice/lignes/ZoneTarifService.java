package com.smartmobility.pricingservice.lignes;

import com.smartmobility.pricingservice.entity.TransportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * ZoneTarifService — calcule le tarif par zones + devis détaillé avec réductions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneTarifService {

    private final LigneRepository ligneRepository;

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
    // Calcul tarif brut (utilisé par FareCalculatorService)
    // ================================================================

    public BigDecimal calculerTarif(TransportType transportType,
                                    String ligneId,
                                    String arretDepartId,
                                    String arretArriveeId) {

        Arret depart = ligneRepository.getArretById(ligneId, arretDepartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt de départ introuvable : " + arretDepartId + " sur ligne " + ligneId));

        Arret arrivee = ligneRepository.getArretById(ligneId, arretArriveeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt d'arrivée introuvable : " + arretArriveeId + " sur ligne " + ligneId));

        int diffZones = Math.abs(arrivee.getNumeroZone() - depart.getNumeroZone());
        BigDecimal tarif = tarifParType(transportType, diffZones);

        log.info("[ZoneTarif] {} | {} → {} | Zone {} → {} | diff={} | tarif={} FCFA",
                transportType, depart.getNom(), arrivee.getNom(),
                depart.getNumeroZone(), arrivee.getNumeroZone(), diffZones, tarif);

        return tarif;
    }

    // ================================================================
    // Devis détaillé — utilisé par GET /pricing/tarif (frontend)
    // ================================================================

    public TarifInfo getTarifInfo(TransportType transportType,
                                  String ligneId,
                                  String arretDepartId,
                                  String arretArriveeId,
                                  String passTier,
                                  int totalTrips) {

        Arret depart = ligneRepository.getArretById(ligneId, arretDepartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt de départ introuvable : " + arretDepartId));

        Arret arrivee = ligneRepository.getArretById(ligneId, arretArriveeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt d'arrivée introuvable : " + arretArriveeId));

        int diffZones = Math.abs(arrivee.getNumeroZone() - depart.getNumeroZone());
        BigDecimal tarifBase = tarifParType(transportType, diffZones);

        // ── Heure serveur ──────────────────────────────────────────
        int currentHour = LocalTime.now().getHour();
        boolean isOffPeak = currentHour >= offPeakStartHour || currentHour < offPeakEndHour;

        // ── Réduction heures creuses ───────────────────────────────
        BigDecimal offPeakDiscount = null;
        String offPeakLabel = null;
        BigDecimal running = tarifBase;

        if (isOffPeak) {
            offPeakDiscount = tarifBase
                    .multiply(offPeakDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            offPeakLabel = "Heures creuses (" + currentHour + "h) -" + offPeakDiscountPercent.intValue() + "%";
            running = running.subtract(offPeakDiscount);
        }

        // ── Réduction tier ─────────────────────────────────────────
        BigDecimal tierDiscount = null;
        String tierLabel = null;

        if (passTier != null && !passTier.equalsIgnoreCase("STANDARD") && !passTier.isBlank()) {
            BigDecimal tierPct = switch (passTier.toUpperCase()) {
                case "SILVER"   -> BigDecimal.valueOf(10);
                case "GOLD"     -> BigDecimal.valueOf(15);
                case "PLATINUM" -> BigDecimal.valueOf(30);
                default         -> BigDecimal.ZERO;
            };
            if (tierPct.compareTo(BigDecimal.ZERO) > 0) {
                tierDiscount = running
                        .multiply(tierPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                tierLabel = "Abonnement " + passTier.toUpperCase() + " -" + tierPct.intValue() + "%";
                running = running.subtract(tierDiscount);
            }
        }

        // ── Réduction fidélité ─────────────────────────────────────
        BigDecimal loyaltyDiscount = null;
        String loyaltyLabel = null;

        if (totalTrips >= loyaltyTripsRequired) {
            loyaltyDiscount = running
                    .multiply(loyaltyDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            loyaltyLabel = "Fidélité (" + totalTrips + " trajets) -" + loyaltyDiscountPercent.intValue() + "%";
            running = running.subtract(loyaltyDiscount);
        }

        BigDecimal totalDiscount = tarifBase.subtract(running).max(BigDecimal.ZERO);
        BigDecimal tarifFinal = running.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        log.info("[ZoneTarif] Devis {} | base={} | remises={} | final={} FCFA",
                transportType, tarifBase, totalDiscount, tarifFinal);

        return TarifInfo.builder()
                .arretDepart(depart.getNom())
                .arretArrivee(arrivee.getNom())
                .zoneDepart(depart.getNumeroZone())
                .zoneArrivee(arrivee.getNumeroZone())
                .diffZones(diffZones)
                .transportType(transportType.name())
                .tarifBase(tarifBase)
                .offPeakDiscount(offPeakDiscount)
                .offPeakLabel(offPeakLabel)
                .tierDiscount(tierDiscount)
                .tierLabel(tierLabel)
                .loyaltyDiscount(loyaltyDiscount)
                .loyaltyLabel(loyaltyLabel)
                .totalDiscount(totalDiscount)
                .tarif(tarifFinal)
                .offPeakHour(isOffPeak)
                .currentHour(currentHour)
                .totalTrips(totalTrips)
                .passTier(passTier)
                .build();
    }

    // ================================================================
    // Tarifs par type
    // ================================================================

    private BigDecimal tarifParType(TransportType type, int diffZones) {
        return switch (type) {
            case BUS_CLASSIQUE -> calculerTarifBus(diffZones);
            case BRT           -> calculerTarifBrt(diffZones);
            case TER           -> calculerTarifTer(diffZones);
        };
    }

    private BigDecimal calculerTarifBus(int diffZones) {
        return switch (diffZones) {
            case 0  -> BigDecimal.valueOf(150);
            case 1  -> BigDecimal.valueOf(200);
            case 2  -> BigDecimal.valueOf(250);
            default -> BigDecimal.valueOf(300);
        };
    }

    private BigDecimal calculerTarifBrt(int diffZones) {
        return switch (diffZones) {
            case 0  -> BigDecimal.valueOf(400);
            case 1  -> BigDecimal.valueOf(800);
            default -> BigDecimal.valueOf(1000);
        };
    }

    private BigDecimal calculerTarifTer(int diffZones) {
        return diffZones == 0 ? BigDecimal.valueOf(500) : BigDecimal.valueOf(1000);
    }
}