-- ──────────────────────────────────────────────────────────────────
-- Schema completo de inicializacion para BackEnfriadores (SEI).
--
-- Este script crea la base de datos desde cero y siembra los cuartos
-- y operadores de desarrollo. Se ejecuta MANUALMENTE (psql -f ...),
-- NO se carga en cada arranque de Spring (esa funcion la cumple
-- src/main/resources/schema.sql con migraciones puntuales).
--
-- Ver sql/README.md para instrucciones de uso.
-- ──────────────────────────────────────────────────────────────────

-- ── Cuartos ────────────────────────────────────────────────────────
-- Catalogo fijo de 5 cuartos frios del SEI v3.0.
CREATE TABLE IF NOT EXISTS cuartos (
    id          INTEGER     PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL,
    descripcion TEXT
);

INSERT INTO cuartos (id, nombre, descripcion) VALUES
    (1, 'Cuarto 1', 'Camara de enfriamiento 1'),
    (2, 'Cuarto 2', 'Camara de enfriamiento 2'),
    (3, 'Cuarto 3', 'Camara de enfriamiento 3'),
    (4, 'Cuarto 4', 'Camara de enfriamiento 4'),
    (5, 'Cuarto 5', 'Camara de enfriamiento 5')
ON CONFLICT (id) DO NOTHING;

-- ── Operadores ─────────────────────────────────────────────────────
-- Refleja la entidad com.codespace.simulator.entities.Operador.
-- aprobado_por / timestamp_aprobacion soportan el flujo HACCP de
-- registro dinamico (UsuarioService).
CREATE TABLE IF NOT EXISTS operadores (
    id                    SERIAL       PRIMARY KEY,
    nombre                VARCHAR(100) NOT NULL,
    rol                   VARCHAR(20)  NOT NULL,        -- 'operador' | 'supervisor'
    activo                BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en             TIMESTAMPTZ,
    usuario               VARCHAR(50)  UNIQUE,
    password_hash         VARCHAR(255),
    aprobado_por          INTEGER REFERENCES operadores (id),
    timestamp_aprobacion  TIMESTAMPTZ
);

-- ── Lecturas de temperatura ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lecturas_temperatura (
    id                  BIGSERIAL    PRIMARY KEY,
    cuarto_id           INTEGER      NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL,
    temperatura         DOUBLE PRECISION NOT NULL,
    estado_alarma       VARCHAR(20)  NOT NULL,           -- normal | preventiva | critica
    origen              VARCHAR(50),
    sensor_id           INTEGER,
    timestamp_evento    TIMESTAMPTZ,
    timestamp_registro  TIMESTAMPTZ,
    topic               VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_lt_cuarto_ts
    ON lecturas_temperatura (cuarto_id, timestamp DESC);

-- ── Alarmas ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alarmas (
    id                 BIGSERIAL    PRIMARY KEY,
    cuarto_id          INTEGER      NOT NULL,
    tipo               VARCHAR(20)  NOT NULL,            -- preventiva | critica
    timestamp_inicio   TIMESTAMPTZ  NOT NULL,
    timestamp_fin      TIMESTAMPTZ,                      -- NULL mientras esta activa
    temperatura_pico   DOUBLE PRECISION NOT NULL,
    silenciada_por     INTEGER REFERENCES operadores (id),
    timestamp_silencio TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_alarmas_cuarto_activa
    ON alarmas (cuarto_id) WHERE timestamp_fin IS NULL;

-- ── Eventos de puerta ──────────────────────────────────────────────
-- `tipo` queda como NULLABLE deliberadamente (ver schema.sql junto al
-- jar, que afloja el NOT NULL en bases con el constraint heredado).
CREATE TABLE IF NOT EXISTS eventos_puerta (
    id                  BIGSERIAL    PRIMARY KEY,
    cuarto_id           INTEGER      NOT NULL,
    timestamp           TIMESTAMPTZ  NOT NULL,
    accion              VARCHAR(50)  NOT NULL,
    origen              VARCHAR(50)  NOT NULL,
    temperatura         DOUBLE PRECISION,
    presencia           BOOLEAN,
    alarma_id           INTEGER REFERENCES alarmas (id),
    sensor_id           INTEGER      NOT NULL,
    timestamp_evento    TIMESTAMPTZ,
    timestamp_registro  TIMESTAMPTZ  NOT NULL,
    tipo                VARCHAR(50),
    topic               VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_eventos_puerta_cuarto_ts
    ON eventos_puerta (cuarto_id, timestamp DESC);

-- ── Intervenciones manuales ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS intervenciones_manuales (
    id                BIGSERIAL    PRIMARY KEY,
    operador_id       INTEGER      NOT NULL REFERENCES operadores (id),
    cuarto_id         INTEGER      NOT NULL,
    timestamp         TIMESTAMPTZ  NOT NULL,
    tipo_accion       VARCHAR(50)  NOT NULL,
    descripcion       TEXT,
    alarma_id         INTEGER REFERENCES alarmas (id),
    rol_en_ejecucion  VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_intervenciones_cuarto_ts
    ON intervenciones_manuales (cuarto_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_intervenciones_operador_ts
    ON intervenciones_manuales (operador_id, timestamp DESC);

-- ── Estado de refrigeracion (historial de cambios de potencia) ─────
CREATE TABLE IF NOT EXISTS refrigeracion_estado (
    id                   BIGSERIAL   PRIMARY KEY,
    cuarto_id            INTEGER     NOT NULL,
    potencia_porcentaje  INTEGER     NOT NULL,
    motivo               VARCHAR(40) NOT NULL,            -- NORMAL | PUERTA_ABIERTA | FORZADO_MANUAL
    timestamp            TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refrig_cuarto_ts
    ON refrigeracion_estado (cuarto_id, timestamp DESC);

-- ──────────────────────────────────────────────────────────────────
-- Seed de operadores de desarrollo.
--
-- IMPORTANTE: los hashes BCrypt de las passwords '1234' deben
-- generarse antes de aplicar este seed. Esta plantilla deja
-- PLACEHOLDERS explicitos para evitar inventar hashes invalidos
-- (un hash incorrecto rompe el login silenciosamente).
--
-- Para generar los hashes:
--   - Desde el back: levantar la app y POSTear a /api/usuarios/registro
--     (ver UsuarioService); el back hashea con BCrypt cost por defecto.
--   - Manual: usar cualquier utilidad BCrypt (htpasswd -nbB usuario 1234,
--     pgcrypto crypt('1234', gen_salt('bf')), etc.).
--
-- Reemplazar los placeholders <bcrypt_hash_de_1234> antes de ejecutar.
-- ──────────────────────────────────────────────────────────────────
INSERT INTO operadores (nombre, usuario, password_hash, rol, activo, creado_en) VALUES
    ('Juan Perez',    'jperez', '$2b$10$2ue64kUHCK75qEYAM6dzEuSYH7n9Vej2OAJR2G5wiyoYAK.TwJg3m', 'operador',   TRUE, NOW()),
    ('Ana Lopez',     'alopez', '$2b$10$6VhRlKokUGL5Djki21WZtu3QXd8y2r4UpKx7XAzjm62So1lff7GKG', 'operador',   TRUE, NOW()),
    ('Carlos Ruiz',   'cruiz',  '$2b$10$LI3FnsVi9/mQXD0trTqZeeUmRPWDLjUChB/BvNGTXZrMqae5Tl2Qe', 'supervisor', TRUE, NOW())
ON CONFLICT (usuario) DO NOTHING;
