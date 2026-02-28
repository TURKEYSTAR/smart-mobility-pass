package com.smartmobility.notificationservice.service;

import com.smartmobility.notificationservice.entity.Notification;
import com.smartmobility.notificationservice.entity.NotificationType;
import com.smartmobility.notificationservice.messaging.PricingFallbackEvent;
import com.smartmobility.notificationservice.messaging.TripCompletedEvent;
import com.smartmobility.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Value("${notification-service.seuil.solde-faible:500}")
    private BigDecimal seuilSoldeFaible;

    // ================================================================
    // TRAITEMENT ÉVÉNEMENTS RABBITMQ
    // ================================================================

    @Transactional
    public void handleTripCompleted(TripCompletedEvent event) {
        log.info("[NotificationService] Traitement TripCompleted - tripId={}, userId={}, montant={} FCFA",
                event.getTripId(), event.getUserId(), event.getAmount());

        // Notification trajet complété
        String message = String.format(
                "Trajet %s complété ✅ | Montant débité : %.0f FCFA | Solde restant : %.0f FCFA",
                event.getTransportType(),
                event.getAmount(),
                event.getBalanceAfter()
        );

        Notification tripNotif = Notification.builder()
                .userId(event.getUserId())
                .passId(event.getPassId())
                .tripId(event.getTripId())
                .type(NotificationType.TRIP_COMPLETED)
                .message(message)
                .amount(event.getAmount())
                .balanceAfter(event.getBalanceAfter())
                .build();

        notificationRepository.save(tripNotif);
        log.info("[NotificationService] ✅ Notification TRIP_COMPLETED sauvegardée");

        // Alerte solde faible si nécessaire
        if (event.getBalanceAfter() != null &&
                event.getBalanceAfter().compareTo(seuilSoldeFaible) < 0) {
            handleLowBalance(event);
        }
    }

    @Transactional
    public void handlePricingFallback(PricingFallbackEvent event) {
        log.warn("[NotificationService] Traitement PricingFallback - tripId={}, montant fallback={} FCFA",
                event.getTripId(), event.getUsedFallbackAmount());

        String message = String.format(
                "⚠️ Tarif standard appliqué pour votre trajet %s (%.0f FCFA) — service tarifaire temporairement indisponible.",
                event.getTransportType(),
                event.getUsedFallbackAmount()
        );

        Notification fallbackNotif = Notification.builder()
                .passId(event.getPassId())
                .userId(UUID.randomUUID()) // userId pas dispo dans l'event fallback
                .tripId(event.getTripId())
                .type(NotificationType.PRICING_FALLBACK)
                .message(message)
                .amount(event.getUsedFallbackAmount())
                .build();

        notificationRepository.save(fallbackNotif);
        log.info("[NotificationService] ✅ Notification PRICING_FALLBACK sauvegardée");
    }

    private void handleLowBalance(TripCompletedEvent event) {
        String message = String.format(
                "⚠️ Solde faible ! Il vous reste %.0f FCFA sur votre Mobility Pass. Pensez à recharger.",
                event.getBalanceAfter()
        );

        Notification lowBalanceNotif = Notification.builder()
                .userId(event.getUserId())
                .passId(event.getPassId())
                .tripId(event.getTripId())
                .type(NotificationType.LOW_BALANCE)
                .message(message)
                .balanceAfter(event.getBalanceAfter())
                .build();

        notificationRepository.save(lowBalanceNotif);
        log.warn("[NotificationService] ⚠️ Alerte LOW_BALANCE envoyée - solde : {} FCFA", event.getBalanceAfter());
    }

    // ================================================================
    // API REST - Consultation
    // ================================================================

    public List<Notification> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadByUserId(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Optional<Notification> markAsRead(UUID notificationId) {
        return notificationRepository.findById(notificationId).map(n -> {
            n.setRead(true);
            return notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        log.info("[NotificationService] {} notifications marquées comme lues pour userId={}", unread.size(), userId);
    }
}