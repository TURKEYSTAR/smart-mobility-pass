package com.smartmobility.pricingservice.lignes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ligne {
    private String id;          // ex: "BRT_B1"
    private String nom;         // ex: "B1 - Omnibus"
    private String description; // ex: "Petersen ↔ Préfecture Guédiawaye"
    private List<Arret> arrets; // dans l'ordre du trajet
}
