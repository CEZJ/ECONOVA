package com.econova.midjourney.repository;

import com.econova.midjourney.model.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
