package com.smartmobility.userservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PassEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing-key.pass-suspended}")
    private String passSuspendedKey;

    @Value("${rabbitmq.routing-key.pass-activated}")
    private String passActivatedKey;

    @Value("${rabbitmq.routing-key.recharge-refused}")
    private String rechargeRefusedKey;

    // ── Pass suspendu par admin ───────────────────────────────────────────────
    public void publishPassSuspended(UUID userId, UUID passId,
                                     String passNumber, BigDecimal currentBalance) {
        PassSuspendedEvent event = PassSuspendedEvent.builder()
                .userId(userId)
                .passId(passId)
                .passNumber(passNumber)
                .currentBalance(currentBalance)
                .suspendedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, passSuspendedKey, event);
        log.warn("[PassEventPublisher] ⛔ PASS_SUSPENDED publié - userId={}, pass={}",
                userId, passNumber);
    }

    // ── Pass réactivé par admin ───────────────────────────────────────────────
    public void publishPassActivated(UUID userId, UUID passId,
                                     String passNumber, BigDecimal currentBalance) {
        PassActivatedEvent event = PassActivatedEvent.builder()
                .userId(userId)
                .passId(passId)
                .passNumber(passNumber)
                .currentBalance(currentBalance)
                .activatedAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, passActivatedKey, event);
        log.info("[PassEventPublisher] ✅ PASS_ACTIVATED publié - userId={}, pass={}",
                userId, passNumber);
    }

    // ── Recharge refusée (pass suspendu ou expiré) ────────────────────────────
    public void publishRechargeRefused(UUID userId, UUID passId,
                                       BigDecimal attemptedAmount, String reason) {
        RechargeRefusedEvent event = RechargeRefusedEvent.builder()
                .userId(userId)
                .passId(passId)
                .attemptedAmount(attemptedAmount)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, rechargeRefusedKey, event);
        log.warn("[PassEventPublisher] ❌ RECHARGE_REFUSED publié - userId={}, raison={}",
                userId, reason);
    }
}