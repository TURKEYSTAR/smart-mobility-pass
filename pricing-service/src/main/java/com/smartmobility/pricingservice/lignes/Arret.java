package com.smartmobility.pricingservice.lignes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Arret {
    private String id;       // ex: "BRT_PETERSEN"
    private String nom;      // ex: "Papa Gueye Fall (Petersen)"
    private int numeroZone;  // 1, 2 ou 3
}
