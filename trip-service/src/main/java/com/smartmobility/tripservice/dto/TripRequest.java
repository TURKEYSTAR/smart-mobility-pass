package com.smartmobility.tripservice.dto;

import com.smartmobility.tripservice.entity.TransportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotBlank(message = "L'origine est requise")
    private String origin;

    @NotBlank(message = "La destination est requise")
    private String destination;

    @NotNull(message = "La distance est requise")
    @Positive(message = "La distance doit être positive")
    private Double distanceKm;

    private LocalDateTime departureTime;
}
