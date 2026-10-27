-- MIGRACIÓN V4: TABLA DE TRANSACCIONES DE PAGO DE SERVICIOS Y TIEMPO AIRE (GESTOPAGO)
CREATE TABLE IF NOT EXISTS transacciones_servicio (
    id                          BIGSERIAL PRIMARY KEY,
    cuenta_id                   BIGINT         NOT NULL,
    random_string               VARCHAR(64)    NOT NULL,
    id_servicio                 INTEGER        NOT NULL,
    id_producto                 INTEGER        NOT NULL,
    precio                      NUMERIC(15, 2) NOT NULL,
    telefono_referencia         VARCHAR(100)   NOT NULL,
    bandera                     VARCHAR(30)    NOT NULL,
    distribuidor                INTEGER        NOT NULL DEFAULT 83,
    hora_local                  VARCHAR(30)    NOT NULL,
    payload_firmado_aes         TEXT,
    estatus                     VARCHAR(30)    NOT NULL DEFAULT 'PENDIENTE',
    codigo_respuesta_proveedor  VARCHAR(20),
    mensaje_proveedor           VARCHAR(255),
    folio_proveedor             VARCHAR(100),
    autorizacion_proveedor      VARCHAR(100),
    fecha_creacion              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transacciones_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas(id) ON DELETE RESTRICT,
    CONSTRAINT uq_transacciones_random_string UNIQUE (random_string),
    CONSTRAINT chk_transacciones_precio_positivo CHECK (precio > 0.00)
);

-- ÍNDICES B-TREE PARA CONSULTAS MASIVAS E IDEMPOTENCIA
CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_id ON transacciones_servicio(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_random_string ON transacciones_servicio(random_string);
CREATE INDEX IF NOT EXISTS idx_transacciones_estatus ON transacciones_servicio(estatus);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha_creacion ON transacciones_servicio(fecha_creacion);
CREATE INDEX IF NOT EXISTS idx_transacciones_servicio_producto ON transacciones_servicio(id_servicio, id_producto);
