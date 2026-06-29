package com.econova.midjourney.service.finance;

import com.econova.midjourney.dto.simulation.ScheduleRowResponse;
import com.econova.midjourney.model.GraceType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Amortización.
 * Genera el cronograma de pagos completo usando el sistema francés,
 * con soporte para períodos de gracia (total y parcial), cuota Balloon,
 * y seguros (desgravamen + vehicular).
 */
@Component
public class AmortizationService {

    private static final int SCALE_MONEY = 2;

    // Tasas de seguros mensuales (configurables según negocio de Econova)
    private static final BigDecimal LIFE_INSURANCE_MONTHLY_RATE = new BigDecimal("0.00049");     // 0.049% mensual
    private static final BigDecimal VEHICLE_INSURANCE_MONTHLY_RATE = new BigDecimal("0.00028");  // 0.028% mensual

    /**
     * Genera el cronograma completo de amortización francesa.
     *
     * @param financedAmount    Monto financiado (precio - cuota inicial)
     * @param balloonAmount     Monto de la cuota Balloon
     * @param tem               Tasa Efectiva Mensual
     * @param termMonths        Plazo total en meses
     * @param installment       Cuota mensual constante calculada
     * @param vehiclePrice      Precio del vehículo (base para seguro vehicular)
     * @param graceType         Tipo de período de gracia
     * @param gracePeriodCount  Número de meses de gracia
     * @return Lista de filas del cronograma
     */
    public List<ScheduleRowResponse> generateSchedule(
            BigDecimal financedAmount,
            BigDecimal balloonAmount,
            BigDecimal tem,
            int termMonths,
            BigDecimal installment,
            BigDecimal vehiclePrice,
            GraceType graceType,
            int gracePeriodCount,
            LocalDate startDate
    ) {
        List<ScheduleRowResponse> schedule = new ArrayList<>();
        BigDecimal balance = financedAmount;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int month = 1; month <= termMonths; month++) {
            String paymentDate = startDate.plusMonths(month).format(fmt);
            boolean isGracePeriod = month <= gracePeriodCount;
            boolean isLastMonth = month == termMonths;

            // Interés del mes sobre saldo vigente
            BigDecimal interest = balance.multiply(tem).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

            // Seguros
            BigDecimal lifeInsurance = balance.multiply(LIFE_INSURANCE_MONTHLY_RATE)
                    .setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            BigDecimal vehicleInsurance = vehiclePrice.multiply(VEHICLE_INSURANCE_MONTHLY_RATE)
                    .setScale(SCALE_MONEY, RoundingMode.HALF_UP);

            BigDecimal principal;
            BigDecimal monthlyInstallment;
            String graceLabel;

            if (isGracePeriod && graceType == GraceType.TOTAL) {
                // Gracia Total: No se paga cuota ni interés → interés se capitaliza al saldo
                principal = BigDecimal.ZERO.setScale(SCALE_MONEY);
                monthlyInstallment = BigDecimal.ZERO.setScale(SCALE_MONEY);
                graceLabel = "TOTAL";
                // Capitalización: el saldo crece
                balance = balance.add(interest);

            } else if (isGracePeriod && graceType == GraceType.PARCIAL) {
                // Gracia Parcial: Solo se pagan intereses, no se amortiza capital
                principal = BigDecimal.ZERO.setScale(SCALE_MONEY);
                monthlyInstallment = interest;
                graceLabel = "PARCIAL";
                // El saldo se mantiene igual

            } else if (isLastMonth) {
                // Último mes: cuota regular + cuota Balloon + ajuste de centavos
                principal = balance.subtract(balloonAmount).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                monthlyInstallment = principal.add(interest).add(balloonAmount);
                graceLabel = "S";
                balance = BigDecimal.ZERO.setScale(SCALE_MONEY);

            } else {
                // Mes regular: amortización francesa estándar
                principal = installment.subtract(interest).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                monthlyInstallment = installment;
                graceLabel = "S";
                balance = balance.subtract(principal).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

                // Protección contra saldo negativo por redondeo
                if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    principal = principal.add(balance);
                    balance = BigDecimal.ZERO.setScale(SCALE_MONEY);
                }
            }

            BigDecimal totalPayment = monthlyInstallment
                    .add(lifeInsurance)
                    .add(vehicleInsurance)
                    .setScale(SCALE_MONEY, RoundingMode.HALF_UP);

            schedule.add(new ScheduleRowResponse(
                    month,
                    paymentDate,
                    graceLabel,
                    // Opening balance = balance antes de amortizar este mes
                    isGracePeriod && graceType == GraceType.TOTAL
                            ? balance.subtract(interest) // Revertir la capitalización para mostrar saldo de apertura
                            : isLastMonth
                            ? principal.add(balloonAmount) // Saldo de apertura del último mes
                            : balance.add(principal),      // Saldo antes de restar amortización
                    interest,
                    principal,
                    monthlyInstallment,
                    lifeInsurance,
                    vehicleInsurance,
                    totalPayment,
                    balance
            ));
        }

        return schedule;
    }

    /**
     * Extrae los flujos de caja totales (cuota + seguros) para cálculo de VAN/TIR.
     * Los flujos son negativos (salidas de dinero del deudor).
     */
    public List<BigDecimal> extractCashFlows(List<ScheduleRowResponse> schedule) {
        return schedule.stream()
                .map(row -> row.totalPayment().negate())
                .toList();
    }
}
