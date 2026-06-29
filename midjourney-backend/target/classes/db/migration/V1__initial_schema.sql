-- =====================================================
-- MidJourney - Econova
-- Migración Inicial: Esquema de Base de Datos
-- =====================================================

-- Tabla de Usuarios (US-001, US-002)
CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name   VARCHAR(100)    NOT NULL,
    email       VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Tabla de Vehículos (US-003)
CREATE TABLE vehicles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    brand       VARCHAR(80)     NOT NULL,
    model       VARCHAR(80)     NOT NULL,
    year        INT             NOT NULL,
    price       NUMERIC(12, 2)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL DEFAULT 'USD',
    image_url   VARCHAR(500),
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Simulaciones (US-016, US-017)
CREATE TABLE simulations (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id              BIGINT          NOT NULL REFERENCES vehicles(id),
    reference_name          VARCHAR(100)    NOT NULL,

    -- Parámetros de entrada
    vehicle_price           NUMERIC(12, 2)  NOT NULL,
    down_payment            NUMERIC(12, 2)  NOT NULL,
    down_payment_percent    NUMERIC(5, 2)   NOT NULL,
    rate_type               VARCHAR(3)      NOT NULL DEFAULT 'TEA',
    interest_rate           NUMERIC(8, 6)   NOT NULL,
    term_months             INT             NOT NULL,
    balloon_percent         NUMERIC(5, 2)   NOT NULL,
    balloon_amount          NUMERIC(12, 2)  NOT NULL,
    grace_type              VARCHAR(15)     NOT NULL DEFAULT 'SIN_GRACIA',
    grace_period_count      INT             NOT NULL DEFAULT 0,

    -- Parámetros calculados
    financed_amount         NUMERIC(12, 2)  NOT NULL,
    monthly_installment     NUMERIC(12, 2)  NOT NULL,
    tcea                    NUMERIC(12, 6)  NOT NULL,
    van                     NUMERIC(12, 2)  NOT NULL,
    tir                     NUMERIC(12, 6)  NOT NULL,

    -- Cronograma almacenado como JSON
    schedule_json           TEXT,

    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_simulations_user_id ON simulations(user_id);

-- Datos iniciales: Vehículos de demostración
INSERT INTO vehicles (brand, model, year, price, currency, image_url) VALUES
    ('Toyota',  'Corolla',      2026, 25000.00, 'USD', 'https://placehold.co/400x300?text=Toyota+Corolla'),
    ('Tesla',   'Model 3',      2026, 42000.00, 'USD', 'https://placehold.co/400x300?text=Tesla+Model+3'),
    ('Honda',   'Civic',        2025, 23500.00, 'USD', 'https://placehold.co/400x300?text=Honda+Civic'),
    ('Hyundai', 'Tucson',       2026, 32000.00, 'USD', 'https://placehold.co/400x300?text=Hyundai+Tucson'),
    ('Kia',     'Sportage',     2025, 29500.00, 'USD', 'https://placehold.co/400x300?text=Kia+Sportage'),
    ('BMW',     'Serie 3',      2026, 55000.00, 'USD', 'https://placehold.co/400x300?text=BMW+Serie+3'),
    ('Nissan',  'Sentra',       2025, 21000.00, 'USD', 'https://placehold.co/400x300?text=Nissan+Sentra'),
    ('Mazda',   'CX-5',         2026, 34000.00, 'USD', 'https://placehold.co/400x300?text=Mazda+CX-5');
