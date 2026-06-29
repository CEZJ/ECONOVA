package com.econova.midjourney.service;

import com.econova.midjourney.exception.BusinessException;
import com.econova.midjourney.model.Vehicle;
import com.econova.midjourney.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * US-003: Listar todos los vehículos del catálogo.
     */
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    /**
     * US-003: Buscar vehículos por texto (marca/modelo) y precio máximo.
     */
    public List<Vehicle> search(String query, BigDecimal maxPrice) {
        return vehicleRepository.findAll().stream()
                .filter(v -> query == null || query.isBlank()
                        || v.getBrand().toLowerCase().contains(query.toLowerCase())
                        || v.getModel().toLowerCase().contains(query.toLowerCase()))
                .filter(v -> maxPrice == null || v.getPrice().compareTo(maxPrice) <= 0)
                .toList();
    }

    /**
     * Buscar vehículo por ID.
     */
    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Vehículo no encontrado con ID: " + id,
                        HttpStatus.NOT_FOUND
                ));
    }
}
