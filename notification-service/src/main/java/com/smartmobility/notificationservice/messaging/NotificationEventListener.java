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

    // ── Trajets ───────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.trip-started}")
    public void onTripStarted(TripStartedEvent event) {
        log.info("[RabbitMQ] TRIP_STARTED - tripId={}, userId={}", event.getTripId(), event.getUserId());
        try { notificationService.handleTripStarted(event); }
        catch (Exception e) { log.error("[RabbitMQ] TRIP_STARTED : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.trip-completed}")
    public void onTripCompleted(TripCompletedEvent event) {
        log.info("[RabbitMQ] TRIP_COMPLETED - tripId={}, userId={}", event.getTripId(), event.getUserId());
        try { notificationService.handleTripCompleted(event); }
        catch (Exception e) { log.error("[RabbitMQ] TRIP_COMPLETED : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.pricing-fallback}")
    public void onPricingFallback(PricingFallbackEvent event) {
        log.warn("[RabbitMQ] PRICING_FALLBACK - tripId={}", event.getTripId());
        try { notificationService.handlePricingFallback(event); }
        catch (Exception e) { log.error("[RabbitMQ] PRICING_FALLBACK : {}", e.getMessage()); }
    }

    // ── Solde ─────────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.low-balance}")
    public void onLowBalance(LowBalanceEvent event) {
        log.warn("[RabbitMQ] LOW_BALANCE - userId={}, solde={}", event.getUserId(), event.getCurrentBalance());
        try { notificationService.handleLowBalance(event); }
        catch (Exception e) { log.error("[RabbitMQ] LOW_BALANCE : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.insufficient-balance}")
    public void onInsufficientBalance(InsufficientBalanceEvent event) {
        log.warn("[RabbitMQ] INSUFFICIENT_BALANCE - userId={}", event.getUserId());
        try { notificationService.handleInsufficientBalance(event); }
        catch (Exception e) { log.error("[RabbitMQ] INSUFFICIENT_BALANCE : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.daily-limit}")
    public void onDailyLimitReached(DailyLimitReachedEvent event) {
        log.warn("[RabbitMQ] DAILY_LIMIT_REACHED - userId={}", event.getUserId());
        try { notificationService.handleDailyLimitReached(event); }
        catch (Exception e) { log.error("[RabbitMQ] DAILY_LIMIT_REACHED : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.recharge-refused}")
    public void onRechargeRefused(RechargeRefusedEvent event) {
        log.warn("[RabbitMQ] RECHARGE_REFUSED - userId={}", event.getUserId());
        try { notificationService.handleRechargeRefused(event); }
        catch (Exception e) { log.error("[RabbitMQ] RECHARGE_REFUSED : {}", e.getMessage()); }
    }

    // ── Pass ──────────────────────────────────────────────────────────────────

    @RabbitListener(queues = "${rabbitmq.queue.pass-suspended}")
    public void onPassSuspended(PassSuspendedEvent event) {
        log.warn("[RabbitMQ] PASS_SUSPENDED - userId={}, pass={}", event.getUserId(), event.getPassNumber());
        try { notificationService.handlePassSuspended(event); }
        catch (Exception e) { log.error("[RabbitMQ] PASS_SUSPENDED : {}", e.getMessage()); }
    }

    @RabbitListener(queues = "${rabbitmq.queue.pass-activated}")
    public void onPassActivated(PassActivatedEvent event) {
        log.info("[RabbitMQ] PASS_ACTIVATED - userId={}, pass={}", event.getUserId(), event.getPassNumber());
        try { notificationService.handlePassActivated(event); }
        catch (Exception e) { log.error("[RabbitMQ] PASS_ACTIVATED : {}", e.getMessage()); }
    }
}