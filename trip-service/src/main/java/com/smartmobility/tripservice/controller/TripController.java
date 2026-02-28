package com.smartmobility.tripservice.controller;

import com.smartmobility.tripservice.dto.TripRequest;
import com.smartmobility.tripservice.dto.TripResponse;
import com.smartmobility.tripservice.entity.Trip;
import com.smartmobility.tripservice.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;

    /**
     * POST /trips/initiate
     * Flux complet : validation → enregistrement → tarif → débit → notification
     */
    @PostMapping("/initiate")
    public ResponseEntity<TripResponse> initiateTrip(
            @Valid @RequestBody TripRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {

        UUID userId = userIdHeader != null
                ? UUID.fromString(userIdHeader)
                : UUID.randomUUID();

        log.info("[TripController] POST /trips/initiate - UserId={}, PassId={}",
                userId, request.getPassId());

        TripResponse response = tripService.initiateTrip(request, userId);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * GET /trips/{tripId}
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(@PathVariable UUID tripId) {
        log.info("[TripController] GET /trips/{}", tripId);
        return tripService.getTripById(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /trips/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Trip>> getTripsByUser(@PathVariable UUID userId) {
        log.info("[TripController] GET /trips/user/{}", userId);
        return ResponseEntity.ok(tripService.getTripsByUserId(userId));
    }

    /**
     * GET /trips/pass/{passId}
     */
    @GetMapping("/pass/{passId}")
    public ResponseEntity<List<Trip>> getTripsByPass(@PathVariable UUID passId) {
        log.info("[TripController] GET /trips/pass/{}", passId);
        return ResponseEntity.ok(tripService.getTripsByPassId(passId));
    }

    /**
     * GET /trips/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Trip Management Service ✅ - Port 8082");
    }

    @GetMapping
    public ResponseEntity<?> getTousLesTrajets(
            @RequestHeader("X-User-Role") String userRole) {

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès refusé : réservé aux ADMIN"));
        }
        return ResponseEntity.ok(tripService.getTousLesTrajets());
    }
}
