package com.smartmobility.pricingservice.lignes;

import com.smartmobility.pricingservice.entity.TransportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * ZoneTarifService — calcule le tarif en fonction des zones de départ et d'arrivée.
 *
 * BUS_CLASSIQUE :
 *   - Même zone      → 150 FCFA
 *   - Différence 1   → 200 FCFA
 *   - Différence 2   → 250 FCFA
 *   - Différence 3+  → 300 FCFA
 *
 * BRT :
 *   - Même zone      → 400 FCFA
 *   - Différence 1   → 800 FCFA
 *   - Différence 2+  → 1000 FCFA
 *
 * TER (2e classe) :
 *   - Même zone      → 500 FCFA
 *   - Différence 1+  → 1000 FCFA
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneTarifService {

    private final LigneRepository ligneRepository;

    /**
     * Calcule le tarif selon la ligne, l'arrêt de départ et l'arrêt d'arrivée.
     *
     * @param transportType  type de transport
     * @param ligneId        identifiant de la ligne (ex: "BRT_B1")
     * @param arretDepartId  identifiant de l'arrêt de départ
     * @param arretArriveeId identifiant de l'arrêt d'arrivée
     * @return montant en FCFA
     */
    public BigDecimal calculerTarif(TransportType transportType,
                                    String ligneId,
                                    String arretDepartId,
                                    String arretArriveeId) {

        Arret depart  = ligneRepository.getArretById(ligneId, arretDepartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt de départ introuvable : " + arretDepartId + " sur ligne " + ligneId));

        Arret arrivee = ligneRepository.getArretById(ligneId, arretArriveeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt d'arrivée introuvable : " + arretArriveeId + " sur ligne " + ligneId));

        int diffZones = Math.abs(arrivee.getNumeroZone() - depart.getNumeroZone());

        BigDecimal tarif = switch (transportType) {
            case BUS_CLASSIQUE -> calculerTarifBus(diffZones);
            case BRT           -> calculerTarifBrt(diffZones);
            case TER           -> calculerTarifTer(diffZones);
        };

        log.info("[ZoneTarif] {} | {} → {} | Zone {} → Zone {} | diff={} | tarif={} FCFA",
                transportType, depart.getNom(), arrivee.getNom(),
                depart.getNumeroZone(), arrivee.getNumeroZone(), diffZones, tarif);

        return tarif;
    }

    /**
     * Retourne le tarif pour un arrêt donné (utilisé pour la validation avant création du trajet).
     */
    public TarifInfo getTarifInfo(TransportType transportType,
                                   String ligneId,
                                   String arretDepartId,
                                   String arretArriveeId) {

        Arret depart  = ligneRepository.getArretById(ligneId, arretDepartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt de départ introuvable : " + arretDepartId));

        Arret arrivee = ligneRepository.getArretById(ligneId, arretArriveeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Arrêt d'arrivée introuvable : " + arretArriveeId));

        int diffZones = Math.abs(arrivee.getNumeroZone() - depart.getNumeroZone());
        BigDecimal tarif = switch (transportType) {
            case BUS_CLASSIQUE -> calculerTarifBus(diffZones);
            case BRT           -> calculerTarifBrt(diffZones);
            case TER           -> calculerTarifTer(diffZones);
        };

        return TarifInfo.builder()
                .arretDepart(depart.getNom())
                .arretArrivee(arrivee.getNom())
                .zoneDepart(depart.getNumeroZone())
                .zoneArrivee(arrivee.getNumeroZone())
                .diffZones(diffZones)
                .tarif(tarif)
                .transportType(transportType.name())
                .build();
    }

    // ── BUS CLASSIQUE ──────────────────────────────────────────────

    private BigDecimal calculerTarifBus(int diffZones) {
        return switch (diffZones) {
            case 0  -> BigDecimal.valueOf(150);
            case 1  -> BigDecimal.valueOf(200);
            case 2  -> BigDecimal.valueOf(250);
            default -> BigDecimal.valueOf(300);
        };
    }

    // ── BRT ────────────────────────────────────────────────────────

    private BigDecimal calculerTarifBrt(int diffZones) {
        return switch (diffZones) {
            case 0  -> BigDecimal.valueOf(400);
            case 1  -> BigDecimal.valueOf(800);
            default -> BigDecimal.valueOf(1000);
        };
    }

    // ── TER (2e classe) ────────────────────────────────────────────

    private BigDecimal calculerTarifTer(int diffZones) {
        return diffZones == 0
                ? BigDecimal.valueOf(500)
                : BigDecimal.valueOf(1000);
    }
}
