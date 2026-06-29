package com.econova.midjourney.dto.simulation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CalculateRequest(

        @NotNull(message = "El ID del vehículo es obligatorio")
        Long vehicleId,

        @NotNull(message = "El precio del vehículo es obligatorio")
        @DecimalMin(value = "1000.00", message = "El precio mínimo del vehículo es $1,000.00")
        BigDecimal vehiclePrice,

        @NotNull(message = "La cuota inicial es obligatoria")
        @DecimalMin(value = "0.00", message = "La cuota inicial no puede ser negativa")
        BigDecimal downPayment,

        @NotNull(message = "El tipo de tasa es obligatorio")
        String rateType, // "TEA" o "TNA"

        @NotNull(message = "El valor de la tasa es obligatorio")
        @DecimalMin(value = "0.01", message = "La tasa debe ser mayor a 0")
        BigDecimal rateValue,

        @NotNull(message = "El plazo en meses es obligatorio")
        @Min(value = 12, message = "El plazo mínimo es 12 meses")
        Integer termMonths,

        @NotNull(message = "El porcentaje Balloon es obligatorio")
        @DecimalMin(value = "20.0", message = "El porcentaje Balloon mínimo es 20%")
        @DecimalMax(value = "40.0", message = "El porcentaje Balloon máximo es 40%")
        BigDecimal balloonPercent,

        @NotNull(message = "El COK anual es obligatorio")
        @DecimalMin(value = "0.0", message = "El COK no puede ser negativo")
        BigDecimal cokAnnual,

        Integer gracePeriodCount,

        String graceType, // "TOTAL", "PARCIAL", "SIN_GRACIA"

        LocalDate startDate // Fecha de inicio del cronograma (por defecto: hoy)
) {
}
