package com.econova.midjourney.controller;

import com.econova.midjourney.dto.simulation.*;
import com.econova.midjourney.model.Simulation;
import com.econova.midjourney.model.User;
import com.econova.midjourney.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * US-014 / US-015: Procesar parámetros de simulación y retornar
     * cronograma de amortización + indicadores financieros.
     * POST /api/simulations/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<CalculateResponse> calculateSimulation(
            @Valid @RequestBody CalculateRequest request
    ) {
        CalculateResponse response = simulationService.executeCalculation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * US-016: Guardar simulación en el historial del usuario autenticado.
     * POST /api/simulations
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveSimulation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SaveSimulationRequest request
    ) {
        Simulation saved = simulationService.saveSimulation(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "message", "Simulación guardada exitosamente"
        ));
    }

    /**
     * US-017: Obtener historial de simulaciones del usuario autenticado.
     * GET /api/simulations/user
     */
    @GetMapping("/user")
    public ResponseEntity<List<SimulationSummaryResponse>> getUserHistory(
            @AuthenticationPrincipal User user
    ) {
        List<SimulationSummaryResponse> history = simulationService.getUserHistory(user.getId());
        return ResponseEntity.ok(history);
    }

    /**
     * US-017: Eliminar simulación del historial.
     * DELETE /api/simulations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSimulation(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        simulationService.deleteSimulation(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Simulación eliminada exitosamente"));
    }
}
