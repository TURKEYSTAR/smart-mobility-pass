package com.smartmobility.tripservice.controller;

import com.smartmobility.tripservice.dto.TripRequest;
import com.smartmobility.tripservice.dto.TripResponse;
import com.smartmobility.tripservice.entity.Trip;
import com.smartmobility.tripservice.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;

    // ── Initier un trajet → IN_PROGRESS (pas de débit) ────────────────────────
    @PostMapping("/initiate")
    public ResponseEntity<TripResponse> initiateTrip(
            @RequestBody TripRequest request,
            @RequestHeader("X-User-Id") String userId) {

        log.info("[TripController] POST /trips/initiate - userId={}", userId);
        TripResponse response = tripService.initiateTrip(request, UUID.fromString(userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── Terminer un trajet → COMPLETED + débit ────────────────────────────────
    @PutMapping("/{tripId}/complete")
    public ResponseEntity<TripResponse> completeTrip(
            @PathVariable UUID tripId,
            @RequestHeader("X-User-Id") String userId) {

        log.info("[TripController] PUT /trips/{}/complete - userId={}", tripId, userId);
        TripResponse response = tripService.completeTrip(tripId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    // ── Annuler un trajet → CANCELLED (pas de remboursement) ─────────────────
    @PutMapping("/{tripId}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(
            @PathVariable UUID tripId,
            @RequestHeader("X-User-Id") String userId) {

        log.info("[TripController] PUT /trips/{}/cancel - userId={}", tripId, userId);
        TripResponse response = tripService.cancelTrip(tripId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    // ── Mes trajets ───────────────────────────────────────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Trip>> getTripsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(tripService.getTripsByUserId(userId));
    }

    // ── Tous les trajets (admin) ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Trip>> getAllTrips() {
        return ResponseEntity.ok(tripService.getTousLesTrajets());
    }

    // ── Un trajet par ID ──────────────────────────────────────────────────────
    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTripById(@PathVariable UUID tripId) {
        return tripService.getTripById(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}