package com.econova.midjourney.controller;

import com.econova.midjourney.model.Vehicle;
import com.econova.midjourney.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * US-003: Listar/buscar catálogo de vehículos.
     * GET /api/vehicles?search=toyota&maxPrice=30000
     */
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal maxPrice) {
        if (search == null && maxPrice == null) {
            return ResponseEntity.ok(vehicleService.findAll());
        }
        return ResponseEntity.ok(vehicleService.search(search, maxPrice));
    }

    /**
     * Obtener vehículo por ID.
     * GET /api/vehicles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.findById(id));
    }
}
