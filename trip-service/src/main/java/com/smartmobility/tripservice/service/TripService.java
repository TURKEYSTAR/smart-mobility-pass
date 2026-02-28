package com.smartmobility.tripservice.service;

import com.smartmobility.tripservice.client.BillingServiceClient;
import com.smartmobility.tripservice.client.PricingServiceClient;
import com.smartmobility.tripservice.client.UserServiceClient;
import com.smartmobility.tripservice.dto.*;
import com.smartmobility.tripservice.entity.TransportType;
import com.smartmobility.tripservice.entity.Trip;
import com.smartmobility.tripservice.entity.TripStatus;
import com.smartmobility.tripservice.exception.InsufficientBalanceException;
import com.smartmobility.tripservice.exception.PassInactiveException;
import com.smartmobility.tripservice.messaging.TripEventPublisher;
import com.smartmobility.tripservice.repository.TripRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final UserServiceClient userServiceClient;
    private final PricingServiceClient pricingServiceClient;
    private final BillingServiceClient billingServiceClient;
    private final TripEventPublisher eventPublisher;

    private static final BigDecimal MINIMUM_BALANCE = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_BALANCE_THRESHOLD = BigDecimal.valueOf(500);

    private static final Map<TransportType, BigDecimal> FALLBACK_FARES = Map.of(
            TransportType.BUS_CLASSIQUE, BigDecimal.valueOf(200),
            TransportType.BRT,           BigDecimal.valueOf(350),
            TransportType.TER,           BigDecimal.valueOf(500)
    );

    // ================================================================
    // FLUX PRINCIPAL
    // ================================================================

    @Transactional
    public TripResponse initiateTrip(TripRequest request, UUID userId) {
        log.info("[apigateway] ====== DÉBUT DU FLUX TRAJET ======");
        log.info("[apigateway] UserId={}, PassId={}, Type={}, Distance={}km",
                userId, request.getPassId(), request.getTransportType(), request.getDistanceKm());

        PassValidationResponse passInfo = validatePass(request.getPassId());

        Trip trip = createTrip(request, userId);

        FareResultDTO fareResult = calculateFareWithCircuitBreaker(trip, passInfo);

        BillingResponse billing = debitAccount(trip, fareResult.getFinalAmount());

        trip.setComputedFare(fareResult.getFinalAmount());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setArrivalTime(LocalDateTime.now());
        tripRepository.save(trip);

        publishEvents(trip, fareResult, billing);

        log.info("[apigateway] ====== FLUX TRAJET TERMINÉ ✅ ======");

        return buildResponse(trip, fareResult, billing);
    }

    // ================================================================
    // ÉTAPE 1 : Validation du Pass
    // ================================================================

    private PassValidationResponse validatePass(UUID passId) {
        PassValidationResponse passInfo = userServiceClient.validatePass(passId);

        if (!"ACTIVE".equals(passInfo.getStatus())) {
            throw new PassInactiveException(
                    "Pass non valide. Statut actuel : " + passInfo.getStatus()
            );
        }

        if (passInfo.getBalance().compareTo(MINIMUM_BALANCE) < 0) {
            throw new InsufficientBalanceException(
                    "Solde insuffisant : " + passInfo.getBalance() + " FCFA. Minimum requis : " + MINIMUM_BALANCE + " FCFA."
            );
        }

        log.info("[apigateway] Pass valide ✅ - Tier : {}, Solde : {} FCFA",
                passInfo.getTier(), passInfo.getBalance());
        return passInfo;
    }

    // ================================================================
    // ÉTAPE 2 : Création du trajet en base (status = INITIATED)
    // ================================================================

    private Trip createTrip(TripRequest request, UUID userId) {
        Trip trip = Trip.builder()
                .userId(userId)
                .passId(request.getPassId())
                .transportType(request.getTransportType())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .departureTime(request.getDepartureTime() != null
                        ? request.getDepartureTime() : LocalDateTime.now())
                .status(TripStatus.INITIATED)
                .build();

        trip = tripRepository.save(trip);
        log.info("[apigateway] Trajet créé ✅ - TripId={}", trip.getId());
        return trip;
    }

    // ================================================================
    // ÉTAPE 3 : Calcul du tarif avec Circuit Breaker Resilience4J
    // ================================================================

    @CircuitBreaker(name = "pricing-service", fallbackMethod = "pricingFallback")
    @TimeLimiter(name = "pricing-service")
    public FareResultDTO calculateFareWithCircuitBreaker(Trip trip, PassValidationResponse passInfo) {
        log.info("[apigateway] Appel Pricing Service - TripId={}", trip.getId());

        PricingRequest pricingRequest = PricingRequest.builder()
                .tripId(trip.getId())
                .transportType(trip.getTransportType())
                .distanceKm(trip.getDistanceKm())
                .departureTime(trip.getDepartureTime())
                .passId(trip.getPassId())
                .passTier(passInfo.getTier())
                .totalTrips(passInfo.getTotalTrips())
                .build();

        FareResultDTO result = pricingServiceClient.calculateFare(pricingRequest);
        log.info("[apigateway] Tarif calculé ✅ - Base : {} FCFA, Réduction : {} FCFA, Final : {} FCFA",
                result.getBaseAmount(), result.getDiscountAmount(), result.getFinalAmount());
        return result;
    }

    // ================================================================
    // FALLBACK Resilience4J
    // ================================================================

    public FareResultDTO pricingFallback(Trip trip, PassValidationResponse passInfo, Exception ex) {
        log.error("[apigateway] ⚡ CIRCUIT BREAKER OUVERT - Pricing Service indisponible !");
        log.error("[apigateway] Cause : {}", ex.getMessage());

        BigDecimal fallbackAmount = FALLBACK_FARES.getOrDefault(
                trip.getTransportType(), BigDecimal.valueOf(200)
        );

        log.warn("[apigateway] Fallback activé : tarif standard {} FCFA pour {}",
                fallbackAmount, trip.getTransportType());

        trip.setStatus(TripStatus.PENDING_PAYMENT);
        tripRepository.save(trip);

        eventPublisher.publishPricingFallbackUsed(
                trip.getId(), trip.getPassId(), fallbackAmount, trip.getTransportType()
        );

        return FareResultDTO.builder()
                .baseAmount(fallbackAmount)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(fallbackAmount)
                .appliedDiscounts(Collections.emptyList())
                .cappedByDailyLimit(false)
                .fallbackUsed(true)
                .note("Tarif standard appliqué (Pricing Service indisponible)")
                .build();
    }

    // ================================================================
    // ÉTAPE 4 : Débit du compte
    // ================================================================

    private BillingResponse debitAccount(Trip trip, BigDecimal amount) {
        BillingRequest billingRequest = BillingRequest.builder()
                .passId(trip.getPassId())
                .tripId(trip.getId())
                .amount(amount)
                .description("Trajet " + trip.getTransportType()
                        + " : " + trip.getOrigin() + " → " + trip.getDestination())
                .build();

        BillingResponse billing = billingServiceClient.debitAccount(billingRequest);
        log.info("[apigateway] Débit effectué ✅ - TransactionId={}, Solde après : {} FCFA",
                billing.getTransactionId(), billing.getBalanceAfter());
        return billing;
    }

    // ================================================================
    // ÉTAPE 5 : Publication événements RabbitMQ
    // ================================================================

    private void publishEvents(Trip trip, FareResultDTO fareResult, BillingResponse billing) {
        eventPublisher.publishTripCompleted(
                trip.getId(), trip.getUserId(), trip.getPassId(),
                fareResult.getFinalAmount(), billing.getBalanceAfter(),
                trip.getTransportType()
        );

        if (billing.getBalanceAfter().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            log.warn("[apigateway] ⚠️ Solde faible : {} FCFA", billing.getBalanceAfter());
        }
    }

    // ================================================================
    // REQUÊTES DE LECTURE
    // ================================================================

    public List<Trip> getTripsByUserId(UUID userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Trip> getTripById(UUID tripId) {
        return tripRepository.findById(tripId);
    }

    public List<Trip> getTripsByPassId(UUID passId) {
        return tripRepository.findByPassIdOrderByCreatedAtDesc(passId);
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
}
