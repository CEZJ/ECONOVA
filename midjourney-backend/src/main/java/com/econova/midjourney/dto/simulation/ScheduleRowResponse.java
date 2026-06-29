package com.econova.midjourney.dto.simulation;

import java.math.BigDecimal;

public record ScheduleRowResponse(
        int month,
        String graceType,
        BigDecimal openingBalance,
        BigDecimal interest,
        BigDecimal principal,
        BigDecimal installment,
        BigDecimal lifeInsurance,
        BigDecimal vehicleInsurance,
        BigDecimal totalPayment,
        BigDecimal closingBalance
) {
}
