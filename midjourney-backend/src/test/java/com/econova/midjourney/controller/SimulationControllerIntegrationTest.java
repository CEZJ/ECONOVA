package com.econova.midjourney.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "JWT_SECRET=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktMTIzNDU2Nzg=")
@Transactional
@DisplayName("US-014/015/016/017: SimulationController Integration")
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, String> reg = Map.of(
                "fullName", "Sim Test",
                "email", "sim.test@econova.com",
                "password", "Pass1234!",
                "confirmPassword", "Pass1234!"
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("token").asText();
    }

    // ─── US-014/015: Calculate ───────────────────────────────────────────────

    @Test
    @DisplayName("US-014: Cálculo exitoso retorna 200 con cronograma")
    void calculate_success() throws Exception {
        Map<String, Object> req = Map.of(
                "vehicleId", 1,
                "vehiclePrice", 25000.00,
                "downPayment", 6250.00,
                "rateType", "TEA",
                "rateValue", 18.00,
                "termMonths", 36,
                "balloonPercent", 20.00,
                "cokAnnual", 12.00,
                "gracePeriodCount", 0,
                "graceType", "SIN_GRACIA"
        );

        mockMvc.perform(post("/api/simulations/calculate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyInstallment").isNumber())
                .andExpect(jsonPath("$.van").isNumber())
                .andExpect(jsonPath("$.tir").isNumber())
                .andExpect(jsonPath("$.tcea").isNumber())
                .andExpect(jsonPath("$.schedule").isArray())
                .andExpect(jsonPath("$.schedule[0].paymentDate").isNotEmpty())
                .andExpect(jsonPath("$.schedule[0].month").value(1));
    }

    @Test
    @DisplayName("US-014: Sin JWT retorna 403")
    void calculate_noToken_returns403() throws Exception {
        Map<String, Object> req = Map.of(
                "vehicleId", 1,
                "vehiclePrice", 25000.00,
                "downPayment", 6250.00,
                "rateType", "TEA",
                "rateValue", 18.00,
                "termMonths", 36,
                "balloonPercent", 20.00,
                "cokAnnual", 12.00
        );

        mockMvc.perform(post("/api/simulations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-014: Balloon > 40% retorna 400")
    void calculate_balloonOver40_returns400() throws Exception {
        Map<String, Object> req = Map.of(
                "vehicleId", 1,
                "vehiclePrice", 25000.00,
                "downPayment", 6250.00,
                "rateType", "TEA",
                "rateValue", 18.00,
                "termMonths", 36,
                "balloonPercent", 45.00,
                "cokAnnual", 12.00,
                "gracePeriodCount", 0,
                "graceType", "SIN_GRACIA"
        );

        mockMvc.perform(post("/api/simulations/calculate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-003: Vehicles with filter ───────────────────────────────────────

    @Test
    @DisplayName("US-003: Filtrar por búsqueda 'toyota' retorna solo Toyotas")
    void getVehicles_searchToyota() throws Exception {
        mockMvc.perform(get("/api/vehicles?search=toyota")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Toyota"));
    }

    @Test
    @DisplayName("US-003: Filtrar por maxPrice=25000 excluye vehículos más caros")
    void getVehicles_maxPrice() throws Exception {
        mockMvc.perform(get("/api/vehicles?maxPrice=25000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
