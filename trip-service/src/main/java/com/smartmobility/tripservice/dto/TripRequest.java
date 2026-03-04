package com.smartmobility.tripservice.dto;

import com.smartmobility.tripservice.entity.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRequest {

    @NotNull(message = "Le passId est requis")
    private UUID passId;

    @NotNull(message = "Le type de transport est requis")
    private TransportType transportType;

    @NotBlank(message = "La ligne est requise")
    private String ligneId;          // ex: "BRT_B1", "TER_L1", "BUS_L1"

    @NotBlank(message = "L'arrêt de départ est requis")
    private String arretDepartId;    // ex: "BRT_PETERSEN"

    @NotBlank(message = "L'arrêt d'arrivée est requis")
    private String arretArriveeId;   // ex: "BRT_PARCELLES"

    // Noms lisibles — optionnels, renseignés par le frontend
    private String nomArretDepart;
    private String nomArretArrivee;

    private LocalDateTime departureTime;
}