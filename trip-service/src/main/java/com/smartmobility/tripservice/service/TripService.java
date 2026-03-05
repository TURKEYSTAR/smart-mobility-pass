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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final PricingClientWrapper pricingClientWrapper;
    private final BillingServiceClient billingServiceClient;
    private final TripEventPublisher eventPublisher;

    private static final BigDecimal MINIMUM_BALANCE = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_BALANCE_THRESHOLD = BigDecimal.valueOf(500);

    @Value("${trip.daily-limit:5000}")
    private BigDecimal dailyLimit;

    // ================================================================
    // INITIER — débit immédiat + IN_PROGRESS
    // ================================================================

    @Transactional
    public TripResponse initiateTrip(TripRequest request, UUID userId) {
        log.info("[TripService] ====== INITIATION TRAJET ======");
        log.info("[TripService] UserId={}, Type={}, Ligne={}, {} → {}",
                userId, request.getTransportType(), request.getLigneId(),
                request.getArretDepartId(), request.getArretArriveeId());

        // ÉTAPE 1 : Validation du pass
        PassValidationResponse passInfo = validatePass(userId);

        // ÉTAPE 2 : Vérifier le plafond journalier
        verifierPlafondJournalier(userId, passInfo.getPassId());

        // ÉTAPE 3 : Création du trajet en IN_PROGRESS
        UUID resolvedPassId = request.getPassId() != null ? request.getPassId() : passInfo.getPassId();
        Trip trip = Trip.builder()
                .userId(userId)
                .passId(resolvedPassId)
                .transportType(request.getTransportType())
                .ligneId(request.getLigneId())
                .arretDepartId(request.getArretDepartId())
                .arretDepartNom(request.getNomArretDepart())
                .arretArriveeId(request.getArretArriveeId())
                .arretArriveeNom(request.getNomArretArrivee())
                .departureTime(request.getDepartureTime() != null
                        ? request.getDepartureTime() : LocalDateTime.now())
                .status(TripStatus.IN_PROGRESS)
                .build();
        trip = tripRepository.save(trip);
        log.info("[TripService] Trajet créé - TripId={}", trip.getId());

        // ÉTAPE 4 : Calcul du tarif par zones
        int totalTrips = countCompletedTrips(userId);
        String passTier = passInfo.getTier() != null ? passInfo.getTier() : "STANDARD";

        log.info("[TripService] userId={} | tier={} | totalTrips={}", userId, passTier, totalTrips);

        PricingRequest pricingRequest = PricingRequest.builder()
                .tripId(trip.getId())
                .transportType(trip.getTransportType())
                .ligneId(trip.getLigneId())
                .arretDepartId(trip.getArretDepartId())
                .arretArriveeId(trip.getArretArriveeId())
                .departureTime(trip.getDepartureTime())
                .passId(trip.getPassId())
                .passTier(passTier)
                .totalTrips(totalTrips)
                .build();
        FareResultDTO fareResult = pricingClientWrapper.calculateFare(pricingRequest);
        log.info("[TripService] Tarif : Base={} | Réduction={} | Final={} FCFA",
                fareResult.getBaseAmount(), fareResult.getDiscountAmount(), fareResult.getFinalAmount());

        // ÉTAPE 5 : Débit immédiat
        BillingResponse billing = debitAccount(trip, fareResult.getFinalAmount());

        // ÉTAPE 6 : Stocker le tarif calculé
        trip.setComputedFare(fareResult.getFinalAmount());
        tripRepository.save(trip);

        // ÉTAPE 7 : Notifications RabbitMQ
        try {
            String label = trip.getTransportType().name()
                    + (trip.getArretDepartNom() != null ? " — " + trip.getArretDepartNom() + " → " + trip.getArretArriveeNom() : "");

            eventPublisher.publishTripStarted(
                    trip.getId(), userId, resolvedPassId,
                    trip.getArretDepartNom() != null ? trip.getArretDepartNom() : trip.getArretDepartId(),
                    trip.getArretArriveeNom() != null ? trip.getArretArriveeNom() : trip.getArretArriveeId(),
                    fareResult.getFinalAmount(), trip.getTransportType());

            if (fareResult.isFallbackUsed()) {
                eventPublisher.publishPricingFallback(
                        trip.getId(), resolvedPassId,
                        "Pricing Service indisponible — tarif standard appliqué",
                        fareResult.getFinalAmount(), trip.getTransportType());
            }

            if (billing.getBalanceAfter() != null
                    && billing.getBalanceAfter().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
                eventPublisher.publishLowBalance(userId, resolvedPassId, billing.getBalanceAfter());
            }

            verifierEtNotifierPlafondAtteint(userId, resolvedPassId, fareResult.getFinalAmount());

        } catch (Exception e) {
            log.warn("[TripService] RabbitMQ indisponible : {}", e.getMessage());
        }

        log.info("[TripService] ====== TRAJET INITIÉ — {} FCFA débités ======", fareResult.getFinalAmount());
        return buildResponse(trip, fareResult, billing,
                "Trajet démarré  — " + fareResult.getFinalAmount()
                        + " FCFA débités. Appuyez sur 'Terminer' à l'arrivée."
                        + (fareResult.isFallbackUsed()
                        ? " (tarif standard — Pricing Service indisponible)" : ""));
    }

    // ================================================================
    // TERMINER
    // ================================================================

    @Transactional
    public TripResponse completeTrip(UUID tripId, UUID userId) {
        log.info("[TripService] ====== COMPLÉTION TRAJET TripId={} ======", tripId);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trajet introuvable : " + tripId));

        if (!trip.getUserId().equals(userId))
            throw new IllegalArgumentException("Ce trajet ne vous appartient pas.");
        if (trip.getStatus() != TripStatus.IN_PROGRESS)
            throw new IllegalStateException("Impossible de terminer ce trajet. Statut : " + trip.getStatus());

        trip.setStatus(TripStatus.COMPLETED);
        trip.setArrivalTime(LocalDateTime.now());
        tripRepository.save(trip);

        try {
            eventPublisher.publishTripCompleted(
                    trip.getId(), trip.getUserId(), trip.getPassId(),
                    trip.getComputedFare(), null, trip.getTransportType());
        } catch (Exception e) {
            log.warn("[TripService] RabbitMQ indisponible : {}", e.getMessage());
        }

        return TripResponse.builder()
                .tripId(trip.getId()).userId(trip.getUserId()).passId(trip.getPassId())
                .transportType(trip.getTransportType())
                .ligneId(trip.getLigneId()).ligneNom(trip.getLigneNom())
                .arretDepartId(trip.getArretDepartId()).arretDepartNom(trip.getArretDepartNom())
                .arretArriveeId(trip.getArretArriveeId()).arretArriveeNom(trip.getArretArriveeNom())
                .zoneDepart(trip.getZoneDepart()).zoneArrivee(trip.getZoneArrivee())
                .status(trip.getStatus()).computedFare(trip.getComputedFare())
                .createdAt(trip.getCreatedAt()).message("Trajet terminé — Bon voyage !")
                .build();
    }

    // ================================================================
    // ANNULER
    // ================================================================

    @Transactional
    public TripResponse cancelTrip(UUID tripId, UUID userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trajet introuvable : " + tripId));

        if (!trip.getUserId().equals(userId))
            throw new IllegalArgumentException("Ce trajet ne vous appartient pas.");
        if (trip.getStatus() != TripStatus.IN_PROGRESS)
            throw new IllegalStateException("Seul un trajet EN COURS peut être annulé. Statut : " + trip.getStatus());

        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        return TripResponse.builder()
                .tripId(trip.getId()).userId(trip.getUserId()).passId(trip.getPassId())
                .transportType(trip.getTransportType())
                .ligneId(trip.getLigneId()).arretDepartNom(trip.getArretDepartNom())
                .arretArriveeNom(trip.getArretArriveeNom())
                .status(trip.getStatus()).computedFare(trip.getComputedFare())
                .createdAt(trip.getCreatedAt())
                .message("Trajet annulé. Le montant débité ne sera pas remboursé.")
                .build();
    }

    // ================================================================
    // PLAFOND JOURNALIER
    // ================================================================

    private void verifierPlafondJournalier(UUID userId, UUID passId) {
        BigDecimal depenseAujourdhui = getTotalDepenseAujourdhui(userId);
        if (depenseAujourdhui.compareTo(dailyLimit) >= 0) {
            log.warn("[TripService] Plafond atteint - userId={}, dépensé={} FCFA", userId, depenseAujourdhui);
            try {
                eventPublisher.publishDailyLimitReached(userId, passId, dailyLimit, depenseAujourdhui);
            } catch (Exception e) {
                log.warn("[TripService] RabbitMQ indisponible : {}", e.getMessage());
            }
            throw new IllegalStateException(
                    "Plafond journalier atteint (" + dailyLimit + " FCFA). "
                            + "Vous avez déjà dépensé " + depenseAujourdhui + " FCFA aujourd'hui.");
        }
    }

    private void verifierEtNotifierPlafondAtteint(UUID userId, UUID passId, BigDecimal dernierMontant) {
        BigDecimal totalAujourdhui = getTotalDepenseAujourdhui(userId);
        if (totalAujourdhui.compareTo(dailyLimit) >= 0) {
            eventPublisher.publishDailyLimitReached(userId, passId, dailyLimit, totalAujourdhui);
        }
    }

    private BigDecimal getTotalDepenseAujourdhui(UUID userId) {
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();
        LocalDateTime finJournee = debutJournee.plusDays(1);
        return tripRepository
                .findByUserIdAndCreatedAtBetween(userId, debutJournee, finJournee)
                .stream()
                .filter(t -> t.getStatus() != TripStatus.CANCELLED)
                .filter(t -> t.getComputedFare() != null)
                .map(Trip::getComputedFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================================================
    // VALIDATION PASS
    // ================================================================

    private PassValidationResponse validatePass(UUID userId) {
        PassValidationResponse passInfo = userServiceClient.getPassByUserId(
                userId, userId.toString(), "USER");

        if (!"ACTIVE".equals(passInfo.getStatus()))
            throw new PassInactiveException("Pass non valide. Statut : " + passInfo.getStatus());

        if (passInfo.getBalance() == null || passInfo.getBalance().compareTo(MINIMUM_BALANCE) < 0) {
            try {
                eventPublisher.publishInsufficientBalance(userId, passInfo.getPassId(), passInfo.getBalance());
            } catch (Exception e) {
                log.warn("[TripService] RabbitMQ indisponible : {}", e.getMessage());
            }
            throw new InsufficientBalanceException(
                    "Solde insuffisant : " + passInfo.getBalance()
                            + " FCFA. Minimum requis : " + MINIMUM_BALANCE + " FCFA.");
        }

        log.info("[TripService] Pass valide - Solde={} FCFA", passInfo.getBalance());
        return passInfo;
    }

    // ================================================================
    // DÉBIT
    // ================================================================

    private BillingResponse debitAccount(Trip trip, BigDecimal amount) {
        String description = trip.getTransportType().name()
                + (trip.getArretDepartNom() != null
                ? " : " + trip.getArretDepartNom() + " → " + trip.getArretArriveeNom()
                : " : " + trip.getArretDepartId() + " → " + trip.getArretArriveeId());

        BillingRequest billingRequest = BillingRequest.builder()
                .userId(trip.getUserId()).tripId(trip.getId())
                .montant(amount).description(description)
                .build();
        BillingResponse billing = billingServiceClient.debitAccount(billingRequest, "trip-service");
        log.info("[TripService] Débit - Solde après={} FCFA", billing.getBalanceAfter());
        return billing;
    }

    // ================================================================
    // BUILDER RÉPONSE
    // ================================================================

    private TripResponse buildResponse(Trip trip, FareResultDTO fare,
                                       BillingResponse billing, String message) {
        return TripResponse.builder()
                .tripId(trip.getId()).userId(trip.getUserId()).passId(trip.getPassId())
                .transportType(trip.getTransportType())
                .ligneId(trip.getLigneId()).ligneNom(trip.getLigneNom())
                .arretDepartId(trip.getArretDepartId()).arretDepartNom(trip.getArretDepartNom())
                .arretArriveeId(trip.getArretArriveeId()).arretArriveeNom(trip.getArretArriveeNom())
                .zoneDepart(trip.getZoneDepart()).zoneArrivee(trip.getZoneArrivee())
                .status(trip.getStatus())
                .baseAmount(fare.getBaseAmount()).discountAmount(fare.getDiscountAmount())
                .computedFare(fare.getFinalAmount()).balanceAfter(billing.getBalanceAfter())
                .transactionId(billing.getTransactionId())
                .appliedDiscounts(fare.getAppliedDiscounts()).fallbackUsed(fare.isFallbackUsed())
                .createdAt(trip.getCreatedAt()).message(message)
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

    private int countCompletedTrips(UUID userId) {
        return (int) tripRepository.countByUserIdAndStatus(userId, TripStatus.COMPLETED);
    }
}