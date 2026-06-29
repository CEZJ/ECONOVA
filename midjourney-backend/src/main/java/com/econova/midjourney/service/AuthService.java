package com.econova.midjourney.service;

import com.econova.midjourney.config.JwtService;
import com.econova.midjourney.dto.auth.AuthResponse;
import com.econova.midjourney.dto.auth.LoginRequest;
import com.econova.midjourney.dto.auth.RegisterRequest;
import com.econova.midjourney.exception.BusinessException;
import com.econova.midjourney.model.User;
import com.econova.midjourney.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * US-001: Registro de un nuevo usuario.
     * Valida contraseñas, verifica duplicados, cifra con bcrypt.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar que las contraseñas coincidan
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("Las contraseñas no coinciden");
        }

        // Validar email duplicado (retornar 409 Conflict)
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                    "El correo electrónico ya se encuentra registrado",
                    HttpStatus.CONFLICT
            );
        }

        // Crear usuario con contraseña cifrada (bcrypt, costo 10)
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);

        // Generar JWT
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getFullName(),
                user.getEmail(),
                "¡Registro exitoso! Bienvenido a MidJourney"
        );
    }

    /**
     * US-002: Inicio de sesión con validación de credenciales.
     * Mensaje genérico en caso de error para evitar enumeración de usuarios.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(
                        "Correo electrónico o contraseña incorrectos",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(
                    "Correo electrónico o contraseña incorrectos",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getFullName(),
                user.getEmail(),
                "Inicio de sesión exitoso"
        );
    }
}
