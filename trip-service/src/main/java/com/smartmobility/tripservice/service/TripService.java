package com.smartmobility.tripservice.service;

import com.smartmobility.tripservice.client.BillingServiceClient;
import com.smartmobility.tripservice.client.PricingClientWrapper;
import com.smartmobility.tripservice.client.UserServiceClient;
import com.smartmobility.tripservice.dto.*;
import com.smartmobility.tripservice.entity.Trip;
import com.smartmobility.tripservice.entity.TripStatus;
import com.smartmobility.tripservice.exception.InsufficientBalanceException;
import com.smartmobility.tripservice.exception.PassInactiveException;
import com.smartmobility.tripservice.messaging.TripEventPublisher;
import com.smartmobility.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final UserServiceClient userServiceClient;
    private final PricingClientWrapper pricingClientWrapper;   // ← wrapper avec CB
    private final BillingServiceClient billingServiceClient;
    private final TripEventPublisher eventPublisher;

    private static final BigDecimal MINIMUM_BALANCE    = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_BALANCE_THRESHOLD = BigDecimal.valueOf(500);

    // ================================================================
    // FLUX PRINCIPAL
    // ================================================================

    @Transactional
    public TripResponse initiateTrip(TripRequest request, UUID userId) {
        log.info("[TripService] ====== DÉBUT DU FLUX TRAJET ======");
        log.info("[TripService] UserId={}, PassId={}, Type={}, Distance={}km",
                userId, request.getPassId(), request.getTransportType(), request.getDistanceKm());

        // ÉTAPE 1 : Validation du pass via userId
        PassValidationResponse passInfo = validatePass(userId);

        // ÉTAPE 2 : Création du trajet
        Trip trip = createTrip(request, userId, passInfo.getPassId());

        // ÉTAPE 3 : Calcul du tarif (avec circuit breaker via wrapper)
        PricingRequest pricingRequest = PricingRequest.builder()
                .tripId(trip.getId())
                .transportType(trip.getTransportType())
                .distanceKm(trip.getDistanceKm())
                .departureTime(trip.getDepartureTime())
                .passId(trip.getPassId())
                .passTier(passInfo.getTier())
                .totalTrips(passInfo.getTotalTrips())
                .build();

        FareResultDTO fareResult = pricingClientWrapper.calculateFare(pricingRequest);
        log.info("[TripService] Tarif : Base={} | Réduction={} | Final={} FCFA",
                fareResult.getBaseAmount(), fareResult.getDiscountAmount(), fareResult.getFinalAmount());

        // ÉTAPE 4 : Débit
        BillingResponse billing = debitAccount(trip, fareResult.getFinalAmount());

        // ÉTAPE 5 : Mise à jour statut
        trip.setComputedFare(fareResult.getFinalAmount());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setArrivalTime(LocalDateTime.now());
        tripRepository.save(trip);

        // ÉTAPE 6 : Publication événements RabbitMQ
        publishEvents(trip, fareResult, billing);

        log.info("[TripService] ====== FLUX TRAJET TERMINÉ ✅ ======");
        return buildResponse(trip, fareResult, billing);
    }

    // ================================================================
    // ÉTAPE 1 : Validation du Pass
    // ================================================================

    private PassValidationResponse validatePass(UUID userId) {
        PassValidationResponse passInfo = userServiceClient.getPassByUserId(
                userId,
                userId.toString(),
                "ADMIN"
        );

        if (!"ACTIVE".equals(passInfo.getStatus())) {
            throw new PassInactiveException(
                    "Pass non valide. Statut actuel : " + passInfo.getStatus()
            );
        }

        if (passInfo.getBalance() == null || passInfo.getBalance().compareTo(MINIMUM_BALANCE) < 0) {
            throw new InsufficientBalanceException(
                    "Solde insuffisant : " + passInfo.getBalance()
                            + " FCFA. Minimum requis : " + MINIMUM_BALANCE + " FCFA."
            );
        }

        log.info("[TripService] Pass valide ✅ - Solde={} FCFA", passInfo.getBalance());
        return passInfo;
    }

    // ================================================================
    // ÉTAPE 2 : Création du trajet
    // ================================================================

    private Trip createTrip(TripRequest request, UUID userId, UUID passId) {
        // passId depuis user-service si request.getPassId() non fourni
        UUID resolvedPassId = request.getPassId() != null ? request.getPassId() : passId;

        Trip trip = Trip.builder()
                .userId(userId)
                .passId(resolvedPassId)
                .transportType(request.getTransportType())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .departureTime(request.getDepartureTime() != null
                        ? request.getDepartureTime() : LocalDateTime.now())
                .status(TripStatus.INITIATED)
                .build();

        trip = tripRepository.save(trip);
        log.info("[TripService] Trajet créé ✅ - TripId={}", trip.getId());
        return trip;
    }

    // ================================================================
    // ÉTAPE 4 : Débit
    // ================================================================

    private BillingResponse debitAccount(Trip trip, BigDecimal amount) {
        BillingRequest billingRequest = BillingRequest.builder()
                .userId(trip.getUserId())
                .tripId(trip.getId())
                .montant(amount)
                .description("Trajet " + trip.getTransportType()
                        + " : " + trip.getOrigin() + " → " + trip.getDestination())
                .build();

        BillingResponse billing = billingServiceClient.debitAccount(billingRequest, "trip-service");
        log.info("[TripService] Débit ✅ - TransactionId={}, Solde après={} FCFA",
                billing.getTransactionId(), billing.getBalanceAfter());
        return billing;
    }

    // ================================================================
    // ÉTAPE 6 : Publication RabbitMQ
    // ================================================================

    private void publishEvents(Trip trip, FareResultDTO fareResult, BillingResponse billing) {
        try {
            eventPublisher.publishTripCompleted(
                    trip.getId(), trip.getUserId(), trip.getPassId(),
                    fareResult.getFinalAmount(), billing.getBalanceAfter(),
                    trip.getTransportType()
            );

            if (billing.getBalanceAfter() != null
                    && billing.getBalanceAfter().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
                log.warn("[TripService] ⚠️ Solde faible : {} FCFA", billing.getBalanceAfter());
            }
        } catch (Exception e) {
            // RabbitMQ optionnel — ne doit pas faire échouer le trajet
            log.warn("[TripService] RabbitMQ indisponible, événement non publié : {}", e.getMessage());
        }
    }

    // ================================================================
    // BUILDER RÉPONSE
    // ================================================================

    private TripResponse buildResponse(Trip trip, FareResultDTO fare, BillingResponse billing) {
        return TripResponse.builder()
                .tripId(trip.getId())
                .userId(trip.getUserId())
                .passId(trip.getPassId())
                .transportType(trip.getTransportType())
                .origin(trip.getOrigin())
                .destination(trip.getDestination())
                .distanceKm(trip.getDistanceKm())
                .status(trip.getStatus())
                .baseAmount(fare.getBaseAmount())
                .discountAmount(fare.getDiscountAmount())
                .computedFare(fare.getFinalAmount())
                .balanceAfter(billing.getBalanceAfter())
                .transactionId(billing.getTransactionId())
                .appliedDiscounts(fare.getAppliedDiscounts())
                .fallbackUsed(fare.isFallbackUsed())
                .createdAt(trip.getCreatedAt())
                .message(fare.isFallbackUsed()
                        ? "Trajet enregistré avec tarif standard (Pricing Service temporairement indisponible)"
                        : "Trajet enregistré avec succès")
                .build();
    }

    // ================================================================
    // LECTURE
    // ================================================================

    public Optional<Trip> getTripById(UUID tripId) {
        return tripRepository.findById(tripId);
    }

    public List<Trip> getTripsByUserId(UUID userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Trip> getTripsByPassId(UUID passId) {
        return tripRepository.findByPassIdOrderByCreatedAtDesc(passId);
    }

    @Transactional(readOnly = true)
    public List<Trip> getTousLesTrajets() {
        return tripRepository.findAll();
    }
}