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
}
