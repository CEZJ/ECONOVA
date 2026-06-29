package com.econova.midjourney.dto.simulation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimulationSummaryResponse(
        Long id,
        String referenceName,
        String vehicleBrand,
        String vehicleModel,
        BigDecimal vehiclePrice,
        BigDecimal monthlyInstallment,
        BigDecimal tcea,
        BigDecimal van,
        BigDecimal tir,
        int termMonths,
        LocalDateTime createdAt
) {
}
