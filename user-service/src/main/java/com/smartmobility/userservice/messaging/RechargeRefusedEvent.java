package com.smartmobility.userservice.messaging;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RechargeRefusedEvent {
    private UUID userId;
    private UUID passId;
    private BigDecimal attemptedAmount;
    private String reason; // "PASS_SUSPENDED" | "PASS_EXPIRED"
    private LocalDateTime occurredAt;
}
