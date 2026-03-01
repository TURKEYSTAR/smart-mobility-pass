package com.smartmobility.userservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassSuspendedEvent {
    private UUID userId;
    private UUID passId;
    private String passNumber;
    private BigDecimal currentBalance;
    private LocalDateTime suspendedAt;
}
