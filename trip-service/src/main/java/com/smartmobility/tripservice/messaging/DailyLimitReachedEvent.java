package com.smartmobility.tripservice.messaging;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyLimitReachedEvent {
    private UUID userId;
    private UUID passId;
    private BigDecimal dailyLimit;
    private BigDecimal totalSpentToday;
    private LocalDateTime occurredAt;
}
