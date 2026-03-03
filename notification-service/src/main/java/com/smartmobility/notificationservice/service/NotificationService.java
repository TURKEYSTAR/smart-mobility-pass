package com.smartmobility.notificationservice.service;

import com.smartmobility.notificationservice.entity.Notification;
import com.smartmobility.notificationservice.entity.NotificationType;
import com.smartmobility.notificationservice.messaging.*;
import com.smartmobility.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ════════════════════════════════════════════════════════════════════════
    // TRAJETS
    // ════════════════════════════════════════════════════════════════════════

    @Transactional
    public void handleTripStarted(TripStartedEvent event) {
        String msg = String.format(
                "Votre trajet %s a démarré — %s → %s | %.0f FCFA débités.",
                event.getTransportType(), event.getOrigin(), event.getDestination(),
                event.getEstimatedFare());

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.TRIP_STARTED).message(msg)
                .amount(event.getEstimatedFare()).build());

        log.info("[NotifService] TRIP_STARTED - userId={}", event.getUserId());
    }

    @Transactional
    public void handleTripCompleted(TripCompletedEvent event) {
        String msg = String.format(
                "Trajet %s terminé | %.0f FCFA débités | Solde restant : %.0f FCFA",
                event.getTransportType(), event.getAmount(),
                event.getBalanceAfter() != null ? event.getBalanceAfter() : BigDecimal.ZERO);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.TRIP_COMPLETED).message(msg)
                .amount(event.getAmount()).balanceAfter(event.getBalanceAfter()).build());

        log.info("[NotifService] TRIP_COMPLETED - userId={}", event.getUserId());
    }

    @Transactional
    public void handlePricingFallback(PricingFallbackEvent event) {
        String msg = String.format(
                "Tarif standard appliqué pour votre trajet %s (%.0f FCFA) — service tarifaire temporairement indisponible.",
                event.getTransportType(), event.getUsedFallbackAmount());

        notificationRepository.save(Notification.builder()
                .userId(event.getPassId()).passId(event.getPassId()).tripId(event.getTripId())
                .type(NotificationType.PRICING_FALLBACK).message(msg)
                .amount(event.getUsedFallbackAmount())
                .build());
        log.warn("[NotifService] PRICING_FALLBACK - userId={}", event.getPassId());
    }

    // ════════════════════════════════════════════════════════════════════════
    // SOLDE
    // ════════════════════════════════════════════════════════════════════════

    @Transactional
    public void handleLowBalance(LowBalanceEvent event) {
        String msg = String.format(
                "Solde faible ! Il vous reste %.0f FCFA sur votre Mobility Pass. Pensez à recharger.",
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.LOW_BALANCE).message(msg)
                .balanceAfter(event.getCurrentBalance()).build());

        log.warn("[NotifService] LOW_BALANCE - userId={}, solde={}", event.getUserId(), event.getCurrentBalance());
    }

    @Transactional
    public void handleInsufficientBalance(InsufficientBalanceEvent event) {
        String msg = String.format(
                "Trajet refusé — Solde insuffisant. Solde actuel : %.0f FCFA. Rechargez votre Mobility Pass.",
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.INSUFFICIENT_BALANCE).message(msg)
                .balanceAfter(event.getCurrentBalance()).build());

        log.warn("[NotifService] INSUFFICIENT_BALANCE - userId={}", event.getUserId());
    }

    @Transactional
    public void handleDailyLimitReached(DailyLimitReachedEvent event) {
        String msg = String.format(
                "Plafond journalier atteint ! Vous avez dépensé %.0f FCFA aujourd'hui (plafond : %.0f FCFA). " +
                        "Vous pourrez effectuer de nouveaux trajets demain.",
                event.getTotalSpentToday(), event.getDailyLimit());

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.DAILY_LIMIT_REACHED).message(msg)
                .amount(event.getTotalSpentToday()).build());

        log.warn("[NotifService] DAILY_LIMIT_REACHED - userId={}, dépensé={} FCFA",
                event.getUserId(), event.getTotalSpentToday());
    }

    @Transactional
    public void handleRechargeRefused(RechargeRefusedEvent event) {
        String reason = "PASS_SUSPENDED".equals(event.getReason())
                ? "votre pass est suspendu"
                : "votre pass est expiré";

        String msg = String.format(
                "Recharge de %.0f FCFA refusée — %s. Contactez un administrateur.",
                event.getAttemptedAmount() != null ? event.getAttemptedAmount() : BigDecimal.ZERO,
                reason);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.RECHARGE_REFUSED).message(msg)
                .amount(event.getAttemptedAmount()).build());

        log.warn("[NotifService] RECHARGE_REFUSED - userId={}, raison={}", event.getUserId(), event.getReason());
    }

    // ════════════════════════════════════════════════════════════════════════
    // PASS
    // ════════════════════════════════════════════════════════════════════════

    @Transactional
    public void handlePassSuspended(PassSuspendedEvent event) {
        String msg = String.format(
                "Votre Mobility Pass %s a été suspendu par un administrateur. " +
                        "Solde actuel : %.0f FCFA. Contactez le support.",
                event.getPassNumber(),
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.PASS_SUSPENDED).message(msg)
                .balanceAfter(event.getCurrentBalance()).build());

        log.warn("[NotifService] PASS_SUSPENDED - userId={}, pass={}", event.getUserId(), event.getPassNumber());
    }

    @Transactional
    public void handlePassActivated(PassActivatedEvent event) {
        String msg = String.format(
                "Votre Mobility Pass %s a été réactivé par un administrateur. " +
                        "Solde disponible : %.0f FCFA. Bon voyage !",
                event.getPassNumber(),
                event.getCurrentBalance() != null ? event.getCurrentBalance() : BigDecimal.ZERO);

        save(Notification.builder()
                .userId(event.getUserId()).passId(event.getPassId())
                .type(NotificationType.PASS_ACTIVATED).message(msg)
                .balanceAfter(event.getCurrentBalance()).build());

        log.info("[NotifService] PASS_ACTIVATED - userId={}, pass={}", event.getUserId(), event.getPassNumber());
    }

    // ════════════════════════════════════════════════════════════════════════
    // LECTURE / GESTION
    // ════════════════════════════════════════════════════════════════════════

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
        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER
    // ════════════════════════════════════════════════════════════════════════

    private Notification save(Notification n) {
        Notification saved = notificationRepository.save(n);
        log.debug("[NotifService] Notification sauvegardée — id={}, type={}, userId={}",
                saved.getId(), saved.getType(), saved.getUserId());
        return saved;
    }
}