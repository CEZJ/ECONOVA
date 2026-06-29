package com.econova.midjourney.dto.simulation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaveSimulationRequest(

        @NotBlank(message = "El nombre de referencia es obligatorio")
        String referenceName,

        @NotNull Long vehicleId,

        // Parámetros de entrada
        @NotNull BigDecimal vehiclePrice,
        @NotNull BigDecimal downPayment,
        @NotNull BigDecimal downPaymentPercent,
        @NotNull String rateType,
        @NotNull BigDecimal interestRate,
        @NotNull Integer termMonths,
        @NotNull BigDecimal balloonPercent,
        @NotNull BigDecimal balloonAmount,
        String graceType,
        Integer gracePeriodCount,

        // Resultados calculados
        @NotNull BigDecimal financedAmount,
        @NotNull BigDecimal monthlyInstallment,
        @NotNull BigDecimal tcea,
        @NotNull BigDecimal van,
        @NotNull BigDecimal tir,

        String scheduleJson
) {
}
