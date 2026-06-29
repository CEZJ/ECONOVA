# MidJourney Backend - Econova

Sistema de Simulación de Crédito Vehicular.

## Stack Tecnológico

- **Java 21** (LTS)
- **Spring Boot 3.3.x**
- **Spring Security + JWT** (jjwt 0.12.6)
- **Spring Data JPA** + **Flyway** (migraciones)
- **PostgreSQL** (producción) / **H2** (desarrollo)
- **Springdoc OpenAPI** (Swagger UI)
- **Lombok**

## Requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (solo para perfil `prod`)

## Cómo Ejecutar

### Modo Desarrollo (H2 en memoria)

```bash
./mvnw spring-boot:run
```

La aplicación levanta en `http://localhost:8080` con base de datos H2 en memoria.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **H2 Console:** http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:midjourneydb`
  - User: `sa` / Password: *(vacío)*

### Modo Producción (PostgreSQL)

1. Crear la base de datos:
```sql
CREATE DATABASE midjourney_db;
CREATE USER midjourney_user WITH PASSWORD 'midjourney_pass';
GRANT ALL PRIVILEGES ON DATABASE midjourney_db TO midjourney_user;
```

2. Ejecutar con perfil `prod`:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

O con variables de entorno:
```bash
DB_USERNAME=midjourney_user DB_PASSWORD=midjourney_pass ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Endpoints de la API

### Autenticación (públicos)

| Método | Endpoint              | Descripción          | US    |
|--------|-----------------------|----------------------|-------|
| POST   | `/api/auth/register`  | Registro de usuario  | US-001|
| POST   | `/api/auth/login`     | Inicio de sesión     | US-002|

### Vehículos (requiere JWT)

| Método | Endpoint              | Descripción               | US    |
|--------|-----------------------|---------------------------|-------|
| GET    | `/api/vehicles`       | Listar catálogo           | US-003|
| GET    | `/api/vehicles/{id}`  | Detalle de vehículo       | US-003|

### Simulaciones (requiere JWT)

| Método | Endpoint                      | Descripción                    | US         |
|--------|-------------------------------|--------------------------------|------------|
| POST   | `/api/simulations/calculate`  | Calcular simulación completa   | US-014/015 |
| POST   | `/api/simulations`            | Guardar simulación             | US-016     |
| GET    | `/api/simulations/user`       | Historial del usuario          | US-017     |
| DELETE | `/api/simulations/{id}`       | Eliminar simulación            | US-017     |

### Headers de Autenticación

```
Authorization: Bearer <token_jwt>
```

## Ejemplo de Uso

### 1. Registrar usuario
```json
POST /api/auth/register
{
  "fullName": "Alejandro Silva",
  "email": "alejandro.silva@econova.com",
  "password": "Econova2026!",
  "confirmPassword": "Econova2026!"
}
```

### 2. Calcular simulación
```json
POST /api/simulations/calculate
{
  "vehicleId": 1,
  "vehiclePrice": 30000.00,
  "downPayment": 6000.00,
  "rateType": "TEA",
  "rateValue": 12.50,
  "termMonths": 36,
  "balloonPercent": 30.00,
  "cokAnnual": 10.00,
  "gracePeriodCount": 0,
  "graceType": "SIN_GRACIA"
}
```

## Estructura del Proyecto

```
src/main/java/com/econova/midjourney/
├── MidJourneyApplication.java
├── config/
│   ├── AppConfig.java          # CORS
│   ├── JwtFilter.java          # Filtro JWT
│   ├── JwtService.java         # Generación/validación JWT
│   └── SecurityConfig.java     # Spring Security
├── controller/
│   ├── AuthController.java     # US-001, US-002
│   ├── VehicleController.java  # US-003
│   └── SimulationController.java # US-014 a US-017
├── dto/
│   ├── auth/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   └── AuthResponse.java
│   └── simulation/
│       ├── CalculateRequest.java
│       ├── CalculateResponse.java
│       ├── ScheduleRowResponse.java
│       ├── SaveSimulationRequest.java
│       └── SimulationSummaryResponse.java
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── model/
│   ├── GraceType.java          # Enum
│   ├── User.java
│   ├── Vehicle.java
│   └── Simulation.java
├── repository/
│   ├── UserRepository.java
│   ├── VehicleRepository.java
│   └── SimulationRepository.java
└── service/
    ├── AuthService.java
    ├── VehicleService.java
    ├── SimulationService.java
    └── finance/
        ├── FinancialEngine.java      # Motor matemático puro
        └── AmortizationService.java  # Generador de cronograma
```

## Fórmulas Financieras Implementadas

| US   | Fórmula                                           |
|------|---------------------------------------------------|
| US-004 | TEM = (1 + TEA)^(30/360) - 1                   |
| US-005 | TED = TNA/360 → TEM = (1 + TED)^30 - 1         |
| US-006 | Monto Financiado = Precio - Cuota Inicial        |
| US-007 | Balloon = Precio × %Balloon                      |
| US-008 | R = (V₀ - B·(1+i)⁻ⁿ) / ((1-(1+i)⁻ⁿ)/i)        |
| US-011 | VAN = Σ(Flujo_t / (1+COK_m)^t)                  |
| US-012 | TIR vía Newton-Raphson                           |
| US-013 | TCEA = (1 + TIR_m)^12 - 1                       |
