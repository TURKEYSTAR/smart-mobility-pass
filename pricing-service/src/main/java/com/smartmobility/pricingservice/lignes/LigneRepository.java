package com.smartmobility.pricingservice.lignes;

import com.smartmobility.pricingservice.entity.TransportType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LigneRepository — données hardcodées des vraies lignes de transport de Dakar.
 *
 * BUS_CLASSIQUE : Lignes Dakar Dem Dikk principales
 * BRT           : Lignes SunuBRT (B1, B2, B3)
 * TER           : Ligne Dakar–Diamniadio (zones officielles)
 */
@Component
public class LigneRepository {

    private static final Map<TransportType, List<Ligne>> LIGNES = new HashMap<>();

    static {
        LIGNES.put(TransportType.BUS_CLASSIQUE, buildBusClassiqueLines());
        LIGNES.put(TransportType.BRT, buildBrtLines());
        LIGNES.put(TransportType.TER, buildTerLines());
    }

    public List<Ligne> getLignesByType(TransportType type) {
        return LIGNES.getOrDefault(type, Collections.emptyList());
    }

    public Optional<Ligne> getLigneById(String ligneId) {
        return LIGNES.values().stream()
                .flatMap(List::stream)
                .filter(l -> l.getId().equals(ligneId))
                .findFirst();
    }

    public Optional<Arret> getArretById(String ligneId, String arretId) {
        return getLigneById(ligneId)
                .flatMap(l -> l.getArrets().stream()
                        .filter(a -> a.getId().equals(arretId))
                        .findFirst());
    }

    // ================================================================
    // BUS CLASSIQUE — Dakar Dem Dikk (lignes principales)
    // Zones : Plateau/Médina (1), Banlieue proche (2), Banlieue lointaine (3), Périphérie (4)
    // ================================================================

    private static List<Ligne> buildBusClassiqueLines() {
        List<Ligne> lignes = new ArrayList<>();

        // Ligne 1 — Dakar Plateau ↔ Pikine
        lignes.add(Ligne.builder()
                .id("BUS_L1")
                .nom("221")
                .description("Dakar Plateau → Sandaga → Colobane → Liberté → Pikine")
                .arrets(List.of(
                        Arret.builder().id("BUS_L1_PLATEAU").nom("Dakar Plateau").numeroZone(1).build(),
                        Arret.builder().id("BUS_L1_SANDAGA").nom("Sandaga").numeroZone(1).build(),
                        Arret.builder().id("BUS_L1_MEDINA").nom("Médina").numeroZone(1).build(),
                        Arret.builder().id("BUS_L1_COLOBANE").nom("Colobane").numeroZone(2).build(),
                        Arret.builder().id("BUS_L1_LIBERTE").nom("Liberté 5").numeroZone(2).build(),
                        Arret.builder().id("BUS_L1_GRAND_YOFF").nom("Grand Yoff").numeroZone(2).build(),
                        Arret.builder().id("BUS_L1_PARCELLES").nom("Parcelles Assainies").numeroZone(3).build(),
                        Arret.builder().id("BUS_L1_PIKINE").nom("Pikine").numeroZone(3).build()
                ))
                .build());

        // Ligne 2 — Dakar Plateau ↔ Guédiawaye
        lignes.add(Ligne.builder()
                .id("BUS_L2")
                .nom("220")
                .description("Dakar Plateau → Fann → HLM → Guédiawaye")
                .arrets(List.of(
                        Arret.builder().id("BUS_L2_PLATEAU").nom("Dakar Plateau").numeroZone(1).build(),
                        Arret.builder().id("BUS_L2_FANN").nom("Fann").numeroZone(1).build(),
                        Arret.builder().id("BUS_L2_HLM").nom("HLM Grand Yoff").numeroZone(2).build(),
                        Arret.builder().id("BUS_L2_CAMBERENE").nom("Cambérène").numeroZone(2).build(),
                        Arret.builder().id("BUS_L2_GOLF_SUD").nom("Golf Sud").numeroZone(3).build(),
                        Arret.builder().id("BUS_L2_GUEDIAWAYE").nom("Guédiawaye").numeroZone(3).build()
                ))
                .build());

        // Ligne 3 — Dakar Plateau ↔ Rufisque
        lignes.add(Ligne.builder()
                .id("BUS_L3")
                .nom("234")
                .description("Dakar Plateau → Thiaroye → Mbao → Rufisque")
                .arrets(List.of(
                        Arret.builder().id("BUS_L3_PLATEAU").nom("Dakar Plateau").numeroZone(1).build(),
                        Arret.builder().id("BUS_L3_GRANDE_MOSQUEE").nom("Grande Mosquée").numeroZone(1).build(),
                        Arret.builder().id("BUS_L3_COLOBANE").nom("Colobane").numeroZone(2).build(),
                        Arret.builder().id("BUS_L3_THIAROYE").nom("Thiaroye").numeroZone(2).build(),
                        Arret.builder().id("BUS_L3_MBAO").nom("Mbao").numeroZone(3).build(),
                        Arret.builder().id("BUS_L3_SANGALKAM").nom("Sangalkam").numeroZone(4).build(),
                        Arret.builder().id("BUS_L3_RUFISQUE").nom("Rufisque").numeroZone(4).build()
                ))
                .build());

        // Ligne 4 — Petersen ↔ Yeumbeul
        lignes.add(Ligne.builder()
                .id("BUS_L4")
                .nom("11")
                .description("Petersen → Liberté → Parcelles → Yeumbeul")
                .arrets(List.of(
                        Arret.builder().id("BUS_L4_PETERSEN").nom("Petersen").numeroZone(1).build(),
                        Arret.builder().id("BUS_L4_SACRE_COEUR").nom("Sacré Cœur").numeroZone(1).build(),
                        Arret.builder().id("BUS_L4_LIBERTE6").nom("Liberté 6").numeroZone(2).build(),
                        Arret.builder().id("BUS_L4_PARCELLES").nom("Parcelles Assainies").numeroZone(2).build(),
                        Arret.builder().id("BUS_L4_PIKINE_NORD").nom("Pikine Nord").numeroZone(3).build(),
                        Arret.builder().id("BUS_L4_YEUMBEUL").nom("Yeumbeul").numeroZone(3).build()
                ))
                .build());

        // Ligne 5 — Dakar Plateau ↔ Diamniadio (ligne express)
        lignes.add(Ligne.builder()
                .id("BUS_L5")
                .nom("30")
                .description("Dakar Plateau → Rufisque → Bargny → Diamniadio")
                .arrets(List.of(
                        Arret.builder().id("BUS_L5_PLATEAU").nom("Dakar Plateau").numeroZone(1).build(),
                        Arret.builder().id("BUS_L5_THIAROYE").nom("Thiaroye").numeroZone(2).build(),
                        Arret.builder().id("BUS_L5_RUFISQUE").nom("Rufisque").numeroZone(3).build(),
                        Arret.builder().id("BUS_L5_BARGNY").nom("Bargny").numeroZone(3).build(),
                        Arret.builder().id("BUS_L5_DIAMNIADIO").nom("Diamniadio").numeroZone(4).build()
                ))
                .build());

        return lignes;
    }

    // ================================================================
    // BRT — SunuBRT
    // Zones : Zone 1 Plateau/Centre, Zone 2 Médina/Liberté, Zone 3 Parcelles/Guédiawaye
    // Tarifs : même zone=400, +1 zone=800, +2 zones=1000
    // ================================================================

    private static List<Ligne> buildBrtLines() {
        List<Ligne> lignes = new ArrayList<>();

        // B1 — Omnibus (toutes les 14 stations opérationnelles)
        lignes.add(Ligne.builder()
                .id("BRT_B1")
                .nom("B1")
                .description("Toutes les stations")
                .arrets(List.of(
                        // Zone 1 — Plateau / Centre-ville
                        Arret.builder().id("BRT_PETERSEN").nom("Petersen").numeroZone(1).build(),
                        Arret.builder().id("BRT_NATION").nom("Place de la Nation").numeroZone(1).build(),
                        Arret.builder().id("BRT_GRAND_DAKAR").nom("Grand Dakar").numeroZone(1).build(),
                        Arret.builder().id("BRT_SACRE_COEUR").nom("Sacré Cœur").numeroZone(1).build(),
                        // Zone 2 — Médina / Liberté
                        Arret.builder().id("BRT_LIBERTE6").nom("Liberté 6").numeroZone(2).build(),
                        Arret.builder().id("BRT_KHAR_YALLA").nom("Khar Yalla").numeroZone(2).build(),
                        Arret.builder().id("BRT_CARDINAL").nom("Cardinal Hyacinthe Thiandoum").numeroZone(2).build(),
                        Arret.builder().id("BRT_GRAND_MEDINE").nom("Grand Médine").numeroZone(2).build(),
                        // Zone 3 — Parcelles / Guédiawaye
                        Arret.builder().id("BRT_CROISEMENT22").nom("Croisement 22").numeroZone(3).build(),
                        Arret.builder().id("BRT_PARCELLES").nom("Parcelles Assainies").numeroZone(3).build(),
                        Arret.builder().id("BRT_NDINGALA").nom("Ndingala").numeroZone(3).build(),
                        Arret.builder().id("BRT_DALAL_JAMM").nom("Dalal Jàmm").numeroZone(3).build(),
                        Arret.builder().id("BRT_GOLF_NORD").nom("Golf Nord").numeroZone(3).build(),
                        Arret.builder().id("BRT_PREFECTURE").nom("Préfecture Guédiawaye").numeroZone(3).build()
                ))
                .build());

        // B2 — Semi-express (7 stations)
        lignes.add(Ligne.builder()
                .id("BRT_B2")
                .nom("B2")
                .description("7 stations")
                .arrets(List.of(
                        Arret.builder().id("BRT_B2_PETERSEN").nom("Petersen").numeroZone(1).build(),
                        Arret.builder().id("BRT_B2_SACRE_COEUR").nom("Sacré Cœur").numeroZone(1).build(),
                        Arret.builder().id("BRT_B2_GRAND_MEDINE").nom("Grand Médine").numeroZone(2).build(),
                        Arret.builder().id("BRT_B2_PARCELLES").nom("Parcelles Assainies").numeroZone(3).build(),
                        Arret.builder().id("BRT_B2_DALAL_JAMM").nom("Dalal Jàmm").numeroZone(3).build(),
                        Arret.builder().id("BRT_B2_GOLF_NORD").nom("Golf Nord").numeroZone(3).build(),
                        Arret.builder().id("BRT_B2_PREFECTURE").nom("Préfecture Guédiawaye").numeroZone(3).build()
                ))
                .build());

        // B3 — Semi-express heures de pointe
        lignes.add(Ligne.builder()
                .id("BRT_B3")
                .nom("B3")
                .description("heures de pointe uniquement")
                .arrets(List.of(
                        Arret.builder().id("BRT_B3_PETERSEN").nom("Petersen").numeroZone(1).build(),
                        Arret.builder().id("BRT_B3_NATION").nom("Place de la Nation").numeroZone(1).build(),
                        Arret.builder().id("BRT_B3_KHAR_YALLA").nom("Khar Yalla").numeroZone(2).build(),
                        Arret.builder().id("BRT_B3_CROISEMENT22").nom("Croisement 22").numeroZone(3).build(),
                        Arret.builder().id("BRT_B3_PARCELLES").nom("Parcelles Assainies").numeroZone(3).build(),
                        Arret.builder().id("BRT_B3_GOLF_NORD").nom("Golf Nord").numeroZone(3).build(),
                        Arret.builder().id("BRT_B3_PREFECTURE").nom("Préfecture Guédiawaye").numeroZone(3).build()
                ))
                .build());

        return lignes;
    }

    // ================================================================
    // TER — Train Express Régional Dakar–Diamniadio
    // Zones officielles : Zone 1 (Dakar→Thiaroye), Zone 2 (Thiaroye→Bargny), Zone 3 (Bargny→Diamniadio)
    // Tarifs 2e classe : même zone=500, +1 zone=1000
    // ================================================================

    private static List<Ligne> buildTerLines() {
        return List.of(
                Ligne.builder()
                        .id("TER")
                        .nom("TER — Dakar ↔ Diamniadio")
                        .description("13 gares — Zone 1: Dakar→Thiaroye | Zone 2: Thiaroye→Bargny | Zone 3: Bargny→Diamniadio")
                        .arrets(List.of(
                                // Zone 1 — Dakar → Thiaroye
                                Arret.builder().id("TER_DAKAR").nom("Gare de Dakar").numeroZone(1).build(),
                                Arret.builder().id("TER_COLOBANE").nom("Colobane").numeroZone(1).build(),
                                Arret.builder().id("TER_HANN").nom("Hann").numeroZone(1).build(),
                                Arret.builder().id("TER_DALIFORT").nom("Dalifort").numeroZone(1).build(),
                                Arret.builder().id("TER_BM").nom("Baux Maraichers").numeroZone(1).build(),
                                Arret.builder().id("TER_PIKINE").nom("Pikine").numeroZone(1).build(),
                                Arret.builder().id("TER_THIAROYE").nom("Thiaroye").numeroZone(1).build(),
                                // Zone 2 — Thiaroye → Bargny
                                Arret.builder().id("TER_YEUMBEUL").nom("Yeumbeul").numeroZone(2).build(),
                                Arret.builder().id("TER_KEUR_MBAYE_FALL").nom("Keur Mbaye Fall").numeroZone(2).build(),
                                Arret.builder().id("TER_PNR").nom("PNR").numeroZone(2).build(),
                                Arret.builder().id("TER_RUFISQUE").nom("Rufisque").numeroZone(2).build(),
                                Arret.builder().id("TER_BARGNY").nom("Bargny").numeroZone(2).build(),
                                // Zone 3 — Bargny → Diamniadio
                                Arret.builder().id("TER_DIAMNIADIO").nom("Diamniadio").numeroZone(3).build()
                        ))
                        .build()
        );
    }
}
