package com.smartmobility.notificationservice.messaging;

import com.smartmobility.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queue.trip-completed}")
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("[RabbitMQ] 📨 Reçu TRIP_COMPLETED - tripId={}, userId={}, montant={} FCFA",
                event.getTripId(), event.getUserId(), event.getAmount());
        try {
            notificationService.handleTripCompleted(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Erreur traitement TRIP_COMPLETED : {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.pricing-fallback}")
    public void onPricingFallback(PricingFallbackEvent event) {
        log.warn("[RabbitMQ] 📨 Reçu PRICING_FALLBACK - tripId={}", event.getTripId());
        try {
            notificationService.handlePricingFallback(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Erreur traitement PRICING_FALLBACK : {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.insufficient-balance}")
    public void onInsufficientBalance(InsufficientBalanceEvent event) {
        log.warn("[RabbitMQ] 📨 Reçu INSUFFICIENT_BALANCE - userId={}, solde={} FCFA",
                event.getUserId(), event.getCurrentBalance());
        try {
            notificationService.handleInsufficientBalance(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Erreur traitement INSUFFICIENT_BALANCE : {}", e.getMessage());
        }
    }

    // ✅ PASS_SUSPENDED — manquait !
    @RabbitListener(queues = "${rabbitmq.queue.pass-suspended}")
    public void onPassSuspended(PassSuspendedEvent event) {
        log.warn("[RabbitMQ] 📨 Reçu PASS_SUSPENDED - userId={}, pass={}",
                event.getUserId(), event.getPassNumber());
        try {
            notificationService.handlePassSuspended(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Erreur traitement PASS_SUSPENDED : {}", e.getMessage());
        }
    }
}