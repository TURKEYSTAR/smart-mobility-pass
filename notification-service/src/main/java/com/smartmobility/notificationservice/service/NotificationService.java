package com.smartmobility.notificationservice.service;

import com.smartmobility.notificationservice.entity.Notification;
import com.smartmobility.notificationservice.entity.NotificationType;
import com.smartmobility.notificationservice.messaging.*;
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

    @Transactional
    public void handleTripCompleted(TripCompletedEvent event) {
        String message = String.format(
                "Trajet %s complété ✅ | Montant débité : %.0f FCFA | Solde restant : %.0f FCFA",
                event.getTransportType(), event.getAmount(), event.getBalanceAfter());

        notificationRepository.save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.TRIP_COMPLETED).message(message)
                .amount(event.getAmount()).balanceAfter(event.getBalanceAfter())
                .build());

        if (event.getBalanceAfter() != null && event.getBalanceAfter().compareTo(seuilSoldeFaible) < 0) {
            handleLowBalance(event);
        }
    }

    @Transactional
    public void handlePricingFallback(PricingFallbackEvent event) {
        String message = String.format(
                "⚠️ Tarif standard appliqué pour votre trajet %s (%.0f FCFA) — service tarifaire temporairement indisponible.",
                event.getTransportType(), event.getUsedFallbackAmount());

        notificationRepository.save(Notification.builder()
                .userId(event.getPassId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.PRICING_FALLBACK).message(message)
                .amount(event.getUsedFallbackAmount())
                .build());
    }

    @Transactional
    public void handleInsufficientBalance(InsufficientBalanceEvent event) {
        String message = String.format(
                "❌ Trajet refusé — Solde insuffisant. Solde actuel : %.0f FCFA. " +
                        "Rechargez votre Mobility Pass pour continuer à voyager.",
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        notificationRepository.save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.INSUFFICIENT_BALANCE).message(message)
                .balanceAfter(event.getCurrentBalance())
                .build());
    }

    // ✅ NOUVEAU
    @Transactional
    public void handlePassSuspended(PassSuspendedEvent event) {
        String message = String.format(
                "⛔ Votre Mobility Pass %s a été suspendu par un administrateur. " +
                        "Solde actuel : %.0f FCFA. Contactez le support pour plus d'informations.",
                event.getPassNumber(),
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        notificationRepository.save(Notification.builder()
                .userId(event.getUserId())
                .passId(event.getPassId())
                .type(NotificationType.PASS_SUSPENDED)
                .message(message)
                .balanceAfter(event.getCurrentBalance())
                .build());

        log.warn("[NotificationService] ⛔ PASS_SUSPENDED sauvegardée - userId={}, pass={}",
                event.getUserId(), event.getPassNumber());
    }

    private void handleLowBalance(TripCompletedEvent event) {
        String message = String.format(
                "⚠️ Solde faible ! Il vous reste %.0f FCFA sur votre Mobility Pass. Pensez à recharger.",
                event.getBalanceAfter());

        notificationRepository.save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.LOW_BALANCE).message(message)
                .balanceAfter(event.getBalanceAfter())
                .build());
    }

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
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}