package com.econova.midjourney.service.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("US-004 a US-013: Motor Financiero")
class FinancialEngineTest {

    private FinancialEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FinancialEngine();
    }

    // ─── US-004: TEA → TEM ───────────────────────────────────────────────────

    @Test
    @DisplayName("US-004: TEA 18% → TEM correcto")
    void convertTeaToTem_18percent() {
        BigDecimal tea = new BigDecimal("0.18");
        BigDecimal tem = engine.convertTeaToTem(tea);
        // (1.18)^(30/360) - 1 ≈ 0.01389
        assertThat(tem.doubleValue()).isCloseTo(0.01389, within(0.0001));
    }

    @Test
    @DisplayName("US-004: TEA 0% → TEM 0%")
    void convertTeaToTem_zero() {
        BigDecimal tem = engine.convertTeaToTem(BigDecimal.ZERO);
        assertThat(tem.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    // ─── US-005: TNA → TEM ───────────────────────────────────────────────────

    @Test
    @DisplayName("US-005: TNA 24% → TEM correcto")
    void convertTnaToTem_24percent() {
        BigDecimal tna = new BigDecimal("0.24");
        BigDecimal tem = engine.convertTnaToTemDaily(tna);
        // TED = 0.24/360 = 0.000667; TEM = (1.000667)^30 - 1 ≈ 0.02022
        assertThat(tem.doubleValue()).isCloseTo(0.02022, within(0.0001));
    }

    // ─── US-006: Monto Financiado ─────────────────────────────────────────────

    @Test
    @DisplayName("US-006: Monto financiado = precio - cuota inicial")
    void calculateFinancedAmount() {
        BigDecimal result = engine.calculateFinancedAmount(
                new BigDecimal("25000.00"), new BigDecimal("5000.00"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    // ─── US-007: Cuota Balloon ───────────────────────────────────────────────

    @Test
    @DisplayName("US-007: Balloon 20% de $25000 = $5000")
    void calculateBalloonAmount_20percent() {
        BigDecimal result = engine.calculateBalloonAmount(
                new BigDecimal("25000.00"), new BigDecimal("0.20"));
        assertThat(result).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    // ─── US-008: Cuota Mensual Francesa ──────────────────────────────────────

    @Test
    @DisplayName("US-008: Cuota mensual calculada es positiva y razonable")
    void calculateFrenchInstallment_basic() {
        BigDecimal installment = engine.calculateFrenchInstallmentWithBalloon(
                new BigDecimal("20000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("0.01389"),
                36
        );
        assertThat(installment.doubleValue()).isGreaterThan(0.0);
        assertThat(installment.doubleValue()).isLessThan(20000.0);
    }

    @Test
    @DisplayName("US-008: Tasa 0% → cuota = (financiado - balloon) / plazo")
    void calculateFrenchInstallment_zeroRate() {
        BigDecimal installment = engine.calculateFrenchInstallmentWithBalloon(
                new BigDecimal("20000.00"),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                15
        );
        // (20000 - 5000) / 15 = 1000
        assertThat(installment).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    // ─── US-011: VAN ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("US-011: VAN con flujos nulos = desembolso inicial")
    void calculateVan_zeroFlows() {
        BigDecimal van = engine.calculateVan(
                new BigDecimal("10000.00"),
                List.of(),
                new BigDecimal("0.01")
        );
        assertThat(van).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("US-011: VAN COK 0% = suma de flujos + desembolso")
    void calculateVan_zeroCok() {
        BigDecimal van = engine.calculateVan(
                new BigDecimal("20000.00"),
                List.of(new BigDecimal("-700"), new BigDecimal("-700"), new BigDecimal("-700")),
                BigDecimal.ZERO
        );
        // 20000 - 700 - 700 - 700 = 17900
        assertThat(van).isEqualByComparingTo(new BigDecimal("17900.00"));
    }

    // ─── US-012: TIR ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("US-012: TIR retorna valor positivo para flujos válidos")
    void calculateTir_validFlows() {
        BigDecimal tir = engine.calculateTir(
                new BigDecimal("10000.00"),
                List.of(
                        new BigDecimal("-350"), new BigDecimal("-350"), new BigDecimal("-350"),
                        new BigDecimal("-350"), new BigDecimal("-350"), new BigDecimal("-350"),
                        new BigDecimal("-350"), new BigDecimal("-350"), new BigDecimal("-350"),
                        new BigDecimal("-350"), new BigDecimal("-350"), new BigDecimal("-10350")
                )
        );
        assertThat(tir.doubleValue()).isGreaterThan(0.0);
        assertThat(tir.doubleValue()).isLessThan(0.1);
    }

    @Test
    @DisplayName("US-012: TIR no convergente retorna 0")
    void calculateTir_nonConvergent_returnsZero() {
        BigDecimal tir = engine.calculateTir(
                new BigDecimal("-99999"),
                List.of(new BigDecimal("-1"), new BigDecimal("-1"))
        );
        assertThat(tir.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }

    // ─── US-013: TCEA ────────────────────────────────────────────────────────

    @Test
    @DisplayName("US-013: TCEA de TIR mensual 1% ≈ 12.68% anual")
    void calculateTcea_1percentMonthly() {
        BigDecimal tcea = engine.calculateTcea(new BigDecimal("0.01"));
        // (1.01)^12 - 1 ≈ 0.1268
        assertThat(tcea.doubleValue()).isCloseTo(0.1268, within(0.0001));
    }

    // ─── Porcentaje cuota inicial ─────────────────────────────────────────────

    @Test
    @DisplayName("Porcentaje cuota inicial: $5000 / $25000 = 20%")
    void calculateDownPaymentPercent() {
        BigDecimal pct = engine.calculateDownPaymentPercent(
                new BigDecimal("5000.00"), new BigDecimal("25000.00"));
        assertThat(pct).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Porcentaje cuota inicial con precio 0 retorna 0")
    void calculateDownPaymentPercent_zeroDivisor() {
        BigDecimal pct = engine.calculateDownPaymentPercent(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(pct.compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }
}
