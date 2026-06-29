package com.econova.midjourney.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "JWT_SECRET=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktMTIzNDU2Nzg=")
@Transactional
@DisplayName("US-001/US-002: AuthController Integration")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─── US-001: Registro ────────────────────────────────────────────────────

    @Test
    @DisplayName("US-001: Registro exitoso retorna 201 + token")
    void register_success() throws Exception {
        Map<String, String> body = Map.of(
                "fullName", "Carlos Test",
                "email", "carlos.test@econova.com",
                "password", "Pass1234!",
                "confirmPassword", "Pass1234!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("carlos.test@econova.com"));
    }

    @Test
    @DisplayName("US-001: Email duplicado retorna 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        Map<String, String> body = Map.of(
                "fullName", "Carlos Test",
                "email", "duplicate@econova.com",
                "password", "Pass1234!",
                "confirmPassword", "Pass1234!"
        );

        // Primer registro
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // Segundo registro con mismo email → 409
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("US-001: Contraseñas no coinciden retorna 400")
    void register_passwordMismatch_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "fullName", "Carlos Test",
                "email", "otro@econova.com",
                "password", "Pass1234!",
                "confirmPassword", "OtraPass!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("US-001: Password sin mayúscula retorna 400 (validación regex)")
    void register_weakPassword_returns400() throws Exception {
        Map<String, String> body = Map.of(
                "fullName", "Carlos Test",
                "email", "weak@econova.com",
                "password", "pass1234!",
                "confirmPassword", "pass1234!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ─── US-002: Login ───────────────────────────────────────────────────────

    @Test
    @DisplayName("US-002: Login exitoso retorna 200 + token")
    void login_success() throws Exception {
        // Registrar usuario primero
        Map<String, String> reg = Map.of(
                "fullName", "Login Test",
                "email", "login@econova.com",
                "password", "Pass1234!",
                "confirmPassword", "Pass1234!"
        );
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        // Login
        Map<String, String> body = Map.of(
                "email", "login@econova.com",
                "password", "Pass1234!"
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("US-002: Password incorrecto retorna 401 con mensaje genérico")
    void login_wrongPassword_returns401() throws Exception {
        Map<String, String> body = Map.of(
                "email", "noexiste@econova.com",
                "password", "Pass1234!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Correo electrónico o contraseña incorrectos"));
    }
}
