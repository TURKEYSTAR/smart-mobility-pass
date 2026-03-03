package com.smartmobility.userservice.messaging;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PassActivatedEvent {
    private UUID userId;
    private UUID passId;
    private String passNumber;
    private BigDecimal currentBalance;
    private LocalDateTime activatedAt;
}
