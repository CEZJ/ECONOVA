package com.econova.midjourney.service.finance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Motor Financiero Puro.
 * Encapsula todos los cálculos financieros del sistema MidJourney.
 * Utiliza BigDecimal con redondeo HALF_UP para consistencia con Excel.
 */
@Component
public class FinancialEngine {

    private static final int SCALE_RATE = 8;
    private static final int SCALE_MONEY = 2;

    // ========================================================================
    // US-004: Conversión de TEA a TEM
    // Fórmula: TEM = (1 + TEA)^(30/360) - 1
    // ========================================================================
    public BigDecimal convertTeaToTem(BigDecimal tea) {
        double teaDouble = tea.doubleValue();
        double temDouble = Math.pow(1.0 + teaDouble, 30.0 / 360.0) - 1.0;
        return BigDecimal.valueOf(temDouble).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-005: Conversión de TNA a TEM (capitalización diaria)
    // Fórmulas: TED = TNA / 360 → TEM = (1 + TED)^30 - 1
    // ========================================================================
    public BigDecimal convertTnaToTemDaily(BigDecimal tna) {
        double tnaDouble = tna.doubleValue();
        double tedDouble = tnaDouble / 360.0;
        double temDouble = Math.pow(1.0 + tedDouble, 30.0) - 1.0;
        return BigDecimal.valueOf(temDouble).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-006: Cálculo del Monto Financiado
    // Fórmula: Monto Financiado = Precio Vehículo - Cuota Inicial
    // ========================================================================
    public BigDecimal calculateFinancedAmount(BigDecimal vehiclePrice, BigDecimal downPayment) {
        return vehiclePrice.subtract(downPayment).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-007: Cálculo de la Cuota Balloon
    // Fórmula: Balloon = Precio Vehículo × %Balloon
    // ========================================================================
    public BigDecimal calculateBalloonAmount(BigDecimal vehiclePrice, BigDecimal balloonPercent) {
        return vehiclePrice.multiply(balloonPercent).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // Porcentaje de Cuota Inicial
    // Fórmula: %CI = (Cuota Inicial / Precio Vehículo) × 100
    // ========================================================================
    public BigDecimal calculateDownPaymentPercent(BigDecimal downPayment, BigDecimal vehiclePrice) {
        if (vehiclePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return downPayment
                .divide(vehiclePrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-008: Cálculo de Cuota Mensual (Método Francés con Balloon)
    // Fórmula: R = (V₀ - B × (1+i)^(-n)) / ((1 - (1+i)^(-n)) / i)
    // Donde: V₀ = monto financiado, B = balloon, i = TEM, n = plazo
    // ========================================================================
    public BigDecimal calculateFrenchInstallmentWithBalloon(
            BigDecimal financedAmount,
            BigDecimal balloonAmount,
            BigDecimal tem,
            int term
    ) {
        double v0 = financedAmount.doubleValue();
        double b = balloonAmount.doubleValue();
        double i = tem.doubleValue();
        int n = term;

        if (i == 0) {
            // Caso especial: tasa 0%
            double r = (v0 - b) / n;
            return BigDecimal.valueOf(r).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }

        double discountFactor = Math.pow(1.0 + i, -n);
        double numerator = v0 - b * discountFactor;
        double denominator = (1.0 - discountFactor) / i;
        double r = numerator / denominator;

        return BigDecimal.valueOf(r).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-010: Cálculo de Seguros
    // Desgravamen: sobre saldo inicial amortizable (tasa mensual)
    // Vehicular/Incendios: sobre valor del auto (tasa mensual)
    // ========================================================================
    public BigDecimal calculateLifeInsurance(BigDecimal openingBalance, BigDecimal monthlyRate) {
        return openingBalance.multiply(monthlyRate).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateVehicleInsurance(BigDecimal vehiclePrice, BigDecimal monthlyRate) {
        return vehiclePrice.multiply(monthlyRate).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-011: Cálculo del VAN (Valor Actual Neto)
    // Fórmula: VAN = Desembolso Inicial + Σ(Flujo_t / (1 + COK_m)^t)
    // ========================================================================
    public BigDecimal calculateVan(
            BigDecimal initialDisbursement,
            List<BigDecimal> cashFlows,
            BigDecimal cokMonthly
    ) {
        BigDecimal van = initialDisbursement;
        double r = cokMonthly.doubleValue();

        for (int t = 0; t < cashFlows.size(); t++) {
            double flow = cashFlows.get(t).doubleValue();
            double pvFactor = Math.pow(1.0 + r, t + 1);
            BigDecimal pvFlow = BigDecimal.valueOf(flow / pvFactor);
            van = van.add(pvFlow);
        }

        return van.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // US-012: Cálculo de la TIR (Tasa Interna de Retorno)
    // Método Numérico: Newton-Raphson
    // ========================================================================
    public BigDecimal calculateTir(BigDecimal initialDisbursement, List<BigDecimal> cashFlows) {
        double x0 = 0.01; // Semilla inicial (1%)
        double precision = 1e-7;
        int maxIterations = 150;

        double[] flows = new double[cashFlows.size() + 1];
        flows[0] = initialDisbursement.doubleValue();
        for (int j = 0; j < cashFlows.size(); j++) {
            flows[j + 1] = cashFlows.get(j).doubleValue();
        }

        double r = x0;
        for (int iter = 0; iter < maxIterations; iter++) {
            double f = 0.0;
            double df = 0.0;

            for (int t = 0; t < flows.length; t++) {
                double discountFactor = Math.pow(1.0 + r, t);
                f += flows[t] / discountFactor;
                if (t > 0) {
                    df -= (t * flows[t]) / Math.pow(1.0 + r, t + 1);
                }
            }

            if (Math.abs(df) < 1e-12) {
                break;
            }

            double rNext = r - (f / df);
            if (Math.abs(rNext - r) < precision) {
                return BigDecimal.valueOf(rNext).setScale(SCALE_RATE, RoundingMode.HALF_UP);
            }
            r = rNext;
        }

        return BigDecimal.ZERO; // No convergió
    }

    // ========================================================================
    // US-013: Cálculo de la TCEA (Tasa de Costo Efectivo Anual)
    // Fórmula: TCEA = (1 + TIR_mensual)^12 - 1
    // ========================================================================
    public BigDecimal calculateTcea(BigDecimal tirMonthly) {
        double tirM = tirMonthly.doubleValue();
        double tcea = Math.pow(1.0 + tirM, 12.0) - 1.0;
        return BigDecimal.valueOf(tcea).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    // ========================================================================
    // Conversión de COK anual a COK mensual
    // Fórmula: COK_m = (1 + COK_a)^(1/12) - 1
    // ========================================================================
    public BigDecimal convertCokAnnualToMonthly(BigDecimal cokAnnual) {
        double cokA = cokAnnual.doubleValue();
        double cokM = Math.pow(1.0 + cokA, 1.0 / 12.0) - 1.0;
        return BigDecimal.valueOf(cokM).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }
}
