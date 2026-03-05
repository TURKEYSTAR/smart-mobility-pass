package com.smartmobility.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Entity
@Table(name = "mobility_pass")
public class MobilityPass {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @Column(unique = true, nullable = false)
    private String passNumber;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassStatus status;

    @Getter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal solde;

    @Getter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Getter
    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Getter
    @OneToOne(mappedBy = "mobilityPass")
    private User user;

    public MobilityPass() {}


}