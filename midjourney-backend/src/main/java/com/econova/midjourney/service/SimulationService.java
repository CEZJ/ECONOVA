package com.econova.midjourney.service;

import com.econova.midjourney.dto.simulation.*;
import com.econova.midjourney.exception.BusinessException;
import com.econova.midjourney.model.GraceType;
import com.econova.midjourney.model.Simulation;
import com.econova.midjourney.model.User;
import com.econova.midjourney.model.Vehicle;
import com.econova.midjourney.repository.SimulationRepository;
import com.econova.midjourney.service.finance.AmortizationService;
import com.econova.midjourney.service.finance.FinancialEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final FinancialEngine financialEngine;
    private final AmortizationService amortizationService;
    private final SimulationRepository simulationRepository;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;

    /**
     * US-014/US-015: Ejecuta el pipeline completo de cálculo.
     * Convierte tasas → calcula monto financiado → cuota → cronograma → métricas.
     */
    public CalculateResponse executeCalculation(CalculateRequest request) {
        // 1. Validar cuota inicial entre 20% y 70% del precio (regla de negocio Econova)
        BigDecimal downPaymentPercent = financialEngine.calculateDownPaymentPercent(
                request.downPayment(), request.vehiclePrice()
        );

        BigDecimal minPercent = new BigDecimal("20.00");
        BigDecimal maxPercent = new BigDecimal("70.00");

        if (downPaymentPercent.compareTo(minPercent) < 0 || downPaymentPercent.compareTo(maxPercent) > 0) {
            throw new BusinessException(
                    "La cuota inicial debe estar entre el 20% y el 70% del valor del vehículo. Actual: "
                            + downPaymentPercent + "%"
            );
        }

        // 2. Convertir tasa a TEM según tipo
        BigDecimal rateDecimal = request.rateValue()
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal tem;
        if ("TNA".equalsIgnoreCase(request.rateType())) {
            tem = financialEngine.convertTnaToTemDaily(rateDecimal);       // US-005
        } else {
            tem = financialEngine.convertTeaToTem(rateDecimal);            // US-004
        }

        // 3. Calcular monto financiado (US-006)
        BigDecimal financedAmount = financialEngine.calculateFinancedAmount(
                request.vehiclePrice(), request.downPayment()
        );

        // 4. Calcular cuota Balloon (US-007)
        BigDecimal balloonDecimal = request.balloonPercent()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal balloonAmount = financialEngine.calculateBalloonAmount(
                request.vehiclePrice(), balloonDecimal
        );

        // 5. Determinar gracia
        GraceType graceType = parseGraceType(request.graceType());
        int gracePeriodCount = request.gracePeriodCount() != null ? request.gracePeriodCount() : 0;

        // 6. Calcular cuota mensual francesa con Balloon (US-008)
        BigDecimal installment = financialEngine.calculateFrenchInstallmentWithBalloon(
                financedAmount, balloonAmount, tem, request.termMonths()
        );

        // 7. Generar cronograma de amortización (US-009, US-010, US-015)
        List<ScheduleRowResponse> schedule = amortizationService.generateSchedule(
                financedAmount, balloonAmount, tem, request.termMonths(),
                installment, request.vehiclePrice(), graceType, gracePeriodCount
        );

        // 8. Extraer flujos de caja para VAN y TIR
        List<BigDecimal> cashFlows = amortizationService.extractCashFlows(schedule);
        BigDecimal initialDisbursement = financedAmount; // Monto recibido por el deudor (positivo)

        // 9. Calcular COK mensual
        BigDecimal cokAnnualDecimal = request.cokAnnual()
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal cokMonthly = financialEngine.convertCokAnnualToMonthly(cokAnnualDecimal);

        // 10. Calcular VAN (US-011)
        BigDecimal van = financialEngine.calculateVan(initialDisbursement, cashFlows, cokMonthly);

        // 11. Calcular TIR (US-012)
        BigDecimal tir = financialEngine.calculateTir(initialDisbursement, cashFlows);

        // 12. Calcular TCEA (US-013)
        BigDecimal tcea = financialEngine.calculateTcea(tir);

        return new CalculateResponse(
                request.vehiclePrice(),
                request.downPayment(),
                downPaymentPercent,
                financedAmount,
                balloonAmount,
                request.rateType(),
                request.rateValue(),
                tem,
                request.termMonths(),
                graceType.name(),
                gracePeriodCount,
                installment,
                van,
                tir,
                tcea,
                schedule
        );
    }

    /**
     * US-016: Guardar simulación en el historial del usuario.
     */
    @Transactional
    public Simulation saveSimulation(User user, SaveSimulationRequest request) {
        Vehicle vehicle = vehicleService.findById(request.vehicleId());

        String scheduleJson = request.scheduleJson();

        Simulation simulation = Simulation.builder()
                .user(user)
                .vehicle(vehicle)
                .referenceName(request.referenceName())
                .vehiclePrice(request.vehiclePrice())
                .downPayment(request.downPayment())
                .downPaymentPercent(request.downPaymentPercent())
                .rateType(request.rateType())
                .interestRate(request.interestRate())
                .termMonths(request.termMonths())
                .balloonPercent(request.balloonPercent())
                .balloonAmount(request.balloonAmount())
                .graceType(parseGraceType(request.graceType()))
                .gracePeriodCount(request.gracePeriodCount() != null ? request.gracePeriodCount() : 0)
                .financedAmount(request.financedAmount())
                .monthlyInstallment(request.monthlyInstallment())
                .tcea(request.tcea())
                .van(request.van())
                .tir(request.tir())
                .scheduleJson(scheduleJson)
                .build();

        return simulationRepository.save(simulation);
    }

    /**
     * US-017: Obtener historial de simulaciones del usuario.
     */
    public List<SimulationSummaryResponse> getUserHistory(Long userId) {
        return simulationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(sim -> new SimulationSummaryResponse(
                        sim.getId(),
                        sim.getReferenceName(),
                        sim.getVehicle().getBrand(),
                        sim.getVehicle().getModel(),
                        sim.getVehiclePrice(),
                        sim.getMonthlyInstallment(),
                        sim.getTcea(),
                        sim.getVan(),
                        sim.getTir(),
                        sim.getTermMonths(),
                        sim.getCreatedAt()
                ))
                .toList();
    }

    /**
     * US-017: Eliminar simulación del historial.
     */
    @Transactional
    public void deleteSimulation(Long simulationId, Long userId) {
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new BusinessException(
                        "Simulación no encontrada", HttpStatus.NOT_FOUND
                ));

        if (!simulation.getUser().getId().equals(userId)) {
            throw new BusinessException(
                    "No tiene permisos para eliminar esta simulación", HttpStatus.FORBIDDEN
            );
        }

        simulationRepository.delete(simulation);
    }

    private GraceType parseGraceType(String graceType) {
        if (graceType == null || graceType.isBlank()) {
            return GraceType.SIN_GRACIA;
        }
        try {
            return GraceType.valueOf(graceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GraceType.SIN_GRACIA;
        }
    }
}
