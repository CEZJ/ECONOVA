package com.econova.midjourney.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 100)
    private String referenceName;

    // --- Parámetros de entrada ---
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vehiclePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal downPayment;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal downPaymentPercent;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String rateType = "TEA";

    @Column(nullable = false, precision = 8, scale = 6)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal balloonPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balloonAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private GraceType graceType = GraceType.SIN_GRACIA;

    @Column(nullable = false)
    @Builder.Default
    private Integer gracePeriodCount = 0;

    // --- Parámetros calculados ---
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal financedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyInstallment;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal tcea;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal van;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal tir;

    @Column(columnDefinition = "TEXT")
    private String scheduleJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
