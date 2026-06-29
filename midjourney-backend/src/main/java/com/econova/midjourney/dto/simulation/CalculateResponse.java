package com.econova.midjourney.dto.simulation;

import java.math.BigDecimal;
import java.util.List;

public record CalculateResponse(
        // Parámetros de entrada reflejados
        BigDecimal vehiclePrice,
        BigDecimal downPayment,
        BigDecimal downPaymentPercent,
        BigDecimal financedAmount,
        BigDecimal balloonAmount,
        String rateType,
        BigDecimal rateValue,
        BigDecimal tem,          // Tasa Efectiva Mensual calculada
        int termMonths,
        String graceType,
        int gracePeriodCount,

        // Métricas financieras calculadas
        BigDecimal monthlyInstallment,
        BigDecimal van,
        BigDecimal tir,
        BigDecimal tcea,

        // Cronograma de amortización
        List<ScheduleRowResponse> schedule
) {
}
