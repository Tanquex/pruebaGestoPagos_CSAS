-- =========================================================================
-- V3__onboarding_clientes.sql
-- Módulo: Onboarding de Clientes Personas Físicas
-- Descripción: Creación de tablas para Domicilios, Clientes, Cuentas y Usuarios
-- Optimizado para alto volumen de registros (100,000+) e integridad relacional.
-- =========================================================================

-- 1. Tabla de Domicilios asociados a los clientes
CREATE TABLE IF NOT EXISTS domicilios (
    id                  BIGSERIAL PRIMARY KEY,
    calle               VARCHAR(150) NOT NULL,
    numero_exterior     VARCHAR(20)  NOT NULL,
    numero_interior     VARCHAR(20),
    colonia             VARCHAR(100) NOT NULL,
    municipio           VARCHAR(100) NOT NULL,
    estado              VARCHAR(100) NOT NULL,
    codigo_postal       CHAR(5)      NOT NULL,
    pais                VARCHAR(50)  NOT NULL DEFAULT 'México',
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. Tabla Principal de Clientes (Personas Físicas)
CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGSERIAL PRIMARY KEY,
    domicilio_id        BIGINT       NOT NULL,
    nombre              VARCHAR(50)  NOT NULL,
    segundo_nombre      VARCHAR(50),
    apellido_paterno    VARCHAR(50)  NOT NULL,
    apellido_materno    VARCHAR(50)  NOT NULL,
    fecha_nacimiento    DATE         NOT NULL,
    curp                CHAR(18)     NOT NULL,
    rfc                 VARCHAR(13)  NOT NULL,
    sexo                VARCHAR(10)  NOT NULL,
    nacionalidad        VARCHAR(50)  NOT NULL DEFAULT 'Mexicana',
    estado_civil        VARCHAR(20)  NOT NULL,
    correo              VARCHAR(100) NOT NULL,
    telefono_movil      CHAR(10)     NOT NULL,
    telefono_alternativo VARCHAR(15),
    ocupacion           VARCHAR(100) NOT NULL,
    empresa             VARCHAR(100) NOT NULL,
    ingreso_mensual     NUMERIC(15, 2) NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_clientes_domicilio FOREIGN KEY (domicilio_id) REFERENCES domicilios(id) ON DELETE RESTRICT,
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo)
);

-- 3. Tabla de Cuentas Bancarias asociadas al cliente (1 a N)
CREATE TABLE IF NOT EXISTS cuentas (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT         NOT NULL,
    numero_cuenta       VARCHAR(20)    NOT NULL,
    saldo               NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    estatus             VARCHAR(20)    NOT NULL DEFAULT 'ACTIVA',
    activo              BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cuentas_numero UNIQUE (numero_cuenta),
    CONSTRAINT chk_cuentas_saldo_no_negativo CHECK (saldo >= 0.00)
);

-- 4. Tabla de Usuarios de Acceso asociados al cliente (1 a 1)
CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT       NOT NULL,
    correo              VARCHAR(100) NOT NULL,
    password            TEXT         NOT NULL,
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_usuarios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_usuarios_correo UNIQUE (correo)
);

-- =========================================================================
-- ÍNDICES B-TREE PARA ALTO RENDIMIENTO (OPTIMIZACIÓN PARA 100,000+ REGISTROS)
-- =========================================================================

-- Índices en Clientes
CREATE INDEX IF NOT EXISTS idx_clientes_curp_lookup ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc_lookup ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo_lookup ON clientes(correo);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
CREATE INDEX IF NOT EXISTS idx_clientes_domicilio ON clientes(domicilio_id);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion ON clientes(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_clientes_busqueda_nombre ON clientes(nombre, apellido_paterno, apellido_materno);

-- Índices en Cuentas
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_lookup ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus ON cuentas(estatus);
CREATE INDEX IF NOT EXISTS idx_cuentas_activo ON cuentas(activo);

-- Índices en Usuarios
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente_id ON usuarios(cliente_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_correo_lookup ON usuarios(correo);
CREATE INDEX IF NOT EXISTS idx_usuarios_activo ON usuarios(activo);
