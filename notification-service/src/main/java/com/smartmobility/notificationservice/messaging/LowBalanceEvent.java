package com.smartmobility.notificationservice.messaging;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LowBalanceEvent {
    private UUID userId;
    private UUID passId;
    private BigDecimal currentBalance;
    private LocalDateTime occurredAt;
}
