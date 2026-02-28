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

    /**
     * Écoute la queue trip.completed.queue
     * Publié par Trip Service après chaque trajet complété
     */
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

    /**
     * Écoute la queue pricing.fallback.queue
     * Publié par Trip Service quand le Pricing Service est indisponible
     */
    @RabbitListener(queues = "${rabbitmq.queue.pricing-fallback}")
    public void onPricingFallback(PricingFallbackEvent event) {
        log.warn("[RabbitMQ] 📨 Reçu PRICING_FALLBACK - tripId={}, montant={} FCFA",
                event.getTripId(), event.getUsedFallbackAmount());
        try {
            notificationService.handlePricingFallback(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] ❌ Erreur traitement PRICING_FALLBACK : {}", e.getMessage());
        }
    }
}