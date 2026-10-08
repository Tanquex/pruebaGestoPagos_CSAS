# Documentación Unitaria: Diseño de Base de Datos y Modelo Físico

Esta documentación detalla el modelo de datos relacional para el **Onboarding de Clientes Personas Físicas**, implementado en **PostgreSQL 16** mediante la migración Flyway [`V3__onboarding_clientes.sql`](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/src/main/resources/db/migration/V3__onboarding_clientes.sql).

---

## 1. Diagrama Entidad-Relación (ERD)

```mermaid
erDiagram
    DOMICILIOS ||--|| CLIENTES : "asociado a (1:1)"
    CLIENTES ||--o{ CUENTAS : "posee (1:N)"
    CLIENTES ||--|| USUARIOS : "autenticación (1:1)"

    DOMICILIOS {
        bigserial id PK
        varchar(150) calle
        varchar(20) numero_exterior
        varchar(20) numero_interior "nullable"
        varchar(100) colonia
        varchar(100) municipio
        varchar(100) estado
        char(5) codigo_postal
        varchar(50) pais
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }

    CLIENTES {
        bigserial id PK
        bigint domicilio_id FK
        varchar(50) nombre
        varchar(50) segundo_nombre "nullable"
        varchar(50) apellido_paterno
        varchar(50) apellido_materno
        date fecha_nacimiento
        char(18) curp UK
        varchar(13) rfc UK
        varchar(10) sexo
        varchar(50) nacionalidad
        varchar(20) estado_civil
        varchar(100) correo UK
        char(10) telefono_movil
        varchar(15) telefono_alternativo "nullable"
        varchar(100) ocupacion
        varchar(100) empresa
        numeric(15,2) ingreso_mensual
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }

    CUENTAS {
        bigserial id PK
        bigint cliente_id FK
        varchar(20) numero_cuenta UK
        numeric(15,2) saldo
        varchar(20) estatus
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }

    USUARIOS {
        bigserial id PK
        bigint cliente_id FK,UK
        varchar(100) correo UK
        text password
        boolean activo
        timestamp fecha_creacion
        timestamp fecha_actualizacion
    }
```

---

## 2. Decisiones de Tipos de Datos y Mejores Prácticas

### ¿Por qué se eligió cada tipo de dato?

1. **Dinero y Contabilidad (`NUMERIC(15, 2)`):**
   * Usado en `clientes.ingreso_mensual` y `cuentas.saldo`.
   * **Justificación técnica:** Los tipos binarios como `float` o `double` acumulan errores de redondeo en operaciones aritméticas (ej. `0.1 + 0.2 = 0.30000000000000004`). El tipo `NUMERIC` en PostgreSQL realiza aritmética decimal exacta, obligatoria en instituciones financieras.
   * Cuenta con la restricción `CHECK (saldo >= 0.00)` para impedir saldos negativos a nivel motor.

2. **Longitudes Fijas (`CHAR(N)`):**
   * `curp`: `CHAR(18)` — La Clave Única de Registro de Población tiene exactamente 18 caracteres alfanuméricos.
   * `telefono_movil`: `CHAR(10)` — La numeración nacional en México consta de exactamente 10 dígitos.
   * `codigo_postal`: `CHAR(5)` — Los códigos postales mexicanos constan de exactamente 5 dígitos numéricos.
   * **Justificación:** Optimiza el almacenamiento interno y descarta de inmediato registros con longitudes incompletas.

3. **Cadenas Variables y Textos Largos (`VARCHAR` vs `TEXT`):**
   * `rfc`: `VARCHAR(13)` — Soporta tanto personas físicas (13 caracteres) como personas morales (12 caracteres).
   * `password`: `TEXT` — Almacena el hash generado por **BCrypt** (60 caracteres de longitud) y deja margen para algoritmos futuros (ej. Argon2) sin requerir alterar la tabla.

4. **Identificadores y Llaves Primarias (`BIGSERIAL` / `BIGINT`):**
   * En sistemas preparados para **100,000+ registros y pruebas masivas**, un entero estándar de 32 bits (`SERIAL` con límite de 2,147,483,647) puede llegar a agotarse en transacciones masivas. `BIGSERIAL` (64 bits) soporta hasta $9 \times 10^{18}$ registros sin desbordamiento.

5. **Fechas Temporales:**
   * `fecha_nacimiento`: `DATE` (solo fecha sin hora, eliminando desfases de zona horaria).
   * `fecha_creacion` / `fecha_actualizacion`: `TIMESTAMP NOT NULL DEFAULT NOW()`.

---

## 3. Estrategia de Indexación B-Tree para 100,000+ Registros

En pruebas de carga masiva (100,000 peticiones), una consulta sin índice genera un *Sequential Scan* (lee todas las páginas del disco). Con índices B-Tree, el costo pasa de $O(N)$ a $O(\log N)$, respondiendo en menos de 2 milisegundos.

| Índice | Tabla | Columnas | Propósito |
| :--- | :--- | :--- | :--- |
| `idx_clientes_curp_lookup` | `clientes` | `curp` (UNIQUE) | Búsqueda instantánea por CURP y prevención de duplicados en concurrencia |
| `idx_clientes_rfc_lookup` | `clientes` | `rfc` (UNIQUE) | Búsqueda instantánea por RFC |
| `idx_clientes_correo_lookup` | `clientes` | `correo` (UNIQUE) | Validación de correo único y búsquedas rápidas |
| `idx_clientes_busqueda_nombre` | `clientes` | `nombre, apellido_paterno, apellido_materno` | Búsqueda compuesta y filtros por nombres/apellidos |
| `idx_clientes_activo` | `clientes` | `activo` | Filtrado instantáneo para listar únicamente clientes con estatus activo |
| `idx_clientes_domicilio` | `clientes` | `domicilio_id` | Join de alto rendimiento entre `clientes` y `domicilios` |
| `idx_clientes_fecha_creacion` | `clientes` | `fecha_creacion` | Consulta rápida de clientes registrados en un rango de fechas |
| `idx_cuentas_numero_lookup` | `cuentas` | `numero_cuenta` (UNIQUE) | Búsqueda por número de cuenta y control de colisiones |
| `idx_cuentas_cliente_id` | `cuentas` | `cliente_id` | Join y consulta de cuentas de un cliente específico |
| `idx_cuentas_estatus` | `cuentas` | `estatus` | Búsqueda de cuentas por estatus (`ACTIVA`, `INACTIVA`) |
| `idx_cuentas_activo` | `cuentas` | `activo` | Filtro de cuentas activas |
| `idx_usuarios_cliente_id` | `usuarios` | `cliente_id` (UNIQUE) | Relación 1 a 1 cliente-usuario |
| `idx_usuarios_correo_lookup` | `usuarios` | `correo` (UNIQUE) | Login instantáneo por correo |

---

## 4. Integridad Relacional y Restricciones
* **Restricciones de Unicidad:** `uq_clientes_curp`, `uq_clientes_rfc`, `uq_clientes_correo`, `uq_cuentas_numero`, `uq_usuarios_correo`.
* **Llaves Foráneas con `ON DELETE RESTRICT`:** Impide que se elimine accidentalmente un cliente si tiene cuentas o usuarios registrados, garantizando que solo se aplique **baja lógica** (`activo = false`).
