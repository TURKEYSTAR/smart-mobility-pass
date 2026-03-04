package com.smartmobility.pricingservice.lignes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifInfo {
    private String arretDepart;
    private String arretArrivee;
    private int zoneDepart;
    private int zoneArrivee;
    private int diffZones;
    private BigDecimal tarif;
    private String transportType;
}
