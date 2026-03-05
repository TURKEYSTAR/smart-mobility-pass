package com.smartmobility.pricingservice.lignes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TarifInfo — devis détaillé retourné au frontend avant d'initier un trajet.
 *
 * Contient le tarif de base par zones + chaque réduction applicable
 * (heures creuses, tier abonnement, fidélité) + le total final.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifInfo {

    // Infos arrêts
    private String arretDepart;
    private String arretArrivee;
    private int zoneDepart;
    private int zoneArrivee;
    private int diffZones;
    private String transportType;

    // Tarif de base (par zones)
    private BigDecimal tarifBase;

    // Réductions
    //Montant de la réduction heures creuses
    private BigDecimal offPeakDiscount;
    private String offPeakLabel;

    // Montant de la réduction tier (SILVER/GOLD/PLATINUM).
    private BigDecimal tierDiscount;
    private String tierLabel;

    // Montant de la réduction fidélité.
    private BigDecimal loyaltyDiscount;
    private String loyaltyLabel;

    // Totaux
    private BigDecimal totalDiscount;
    private BigDecimal tarif;

    // Contexte
    private boolean offPeakHour;
    private int currentHour;
    private int totalTrips;
    private String passTier;
}