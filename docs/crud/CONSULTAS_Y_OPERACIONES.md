# Consultas, Actualizaciones y Bajas Lógicas

Este documento detalla la operativa de consultas (búsquedas simples y paginadas), actualizaciones parciales y el proceso de baja lógica en cascada implementados en la capa de servicios ([ClienteServiceImpl](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/src/main/java/com/proyecto/servicios/service/Impl/ClienteServiceImpl.java) y [CuentaServiceImpl](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/src/main/java/com/proyecto/servicios/service/Impl/CuentaServiceImpl.java)).

---

## 1. Consultas y Búsquedas

Para soportar grandes volúmenes de información y evitar saturación de memoria, todas las operaciones de búsqueda masiva o por coincidencia parcial implementan **paginación con Spring Data (`Pageable`)**.

### 1.1. Búsquedas de Clientes

| Método de Búsqueda | Parámetro | Retorno | Comportamiento / Manejo |
|---|---|---|---|
| **Por ID** | `id` (Long) | Objeto único | Búsqueda por clave primaria $O(1)$. Lanza `ClienteNoEncontradoException` si no existe. |
| **Por CURP** | `curp` (String) | Objeto único | Case-insensitive con B-Tree Index. Lanza `ClienteNoEncontradoException` si no existe. |
| **Por RFC** | `rfc` (String) | Objeto único | Case-insensitive con B-Tree Index. Lanza `ClienteNoEncontradoException` si no existe. |
| **Por Correo** | `correo` (String) | Objeto único | Case-insensitive con B-Tree Index. Lanza `ClienteNoEncontradoException` si no existe. |
| **Por Número de Cuenta** | `numeroCuenta` (String) | Objeto único | Resuelve mediante `JOIN` entre `clientes` y `cuentas`. |
| **Por Nombre** | `nombre` (String) | `Page<ClienteResponseDto>` | Coincidencia parcial con `ILIKE %nombre%` paginado. |
| **Por Apellido Paterno** | `apellidoPaterno` (String) | `Page<ClienteResponseDto>` | Coincidencia parcial con `ILIKE %apellido%` paginado. |
| **Por Apellido Materno** | `apellidoMaterno` (String) | `Page<ClienteResponseDto>` | Coincidencia parcial con `ILIKE %apellido%` paginado. |
| **Por Estatus Activo** | - | `Page<ClienteResponseDto>` | Filtra únicamente `activo = true` paginado. |
| **Por Rango de Fechas** | `inicio`, `fin` | `Page<ClienteResponseDto>` | Filtra por `fecha_creacion BETWEEN :inicio AND :fin` paginado. |

### 1.2. Búsquedas y Operaciones de Cuentas

- **Por Número de Cuenta:** Búsqueda directa por los 16 dígitos únicos.
- **Por Cliente ID:** Retorno de lista completa o páginas de cuentas pertenecientes al cliente.
- **Por Estatus (`ACTIVA`, `INACTIVA`, `BLOQUEADA`, `CANCELADA`):** Filtrado paginado por estatus operativo.
- **Consulta de Saldo Directo:** Retorna exclusivamente el saldo (`BigDecimal`) de una cuenta existente, útil para operaciones rápidas o cajeros/banca móvil.

---

## 2. Actualizaciones Parciales (Reglas de Inmutabilidad)

Las reglas de negocio estipulan que los identificadores legales y bancarios son **inmutables**:

- **Campos Inmutables (NO MODIFICABLES):**
  - **CURP**: Identificador oficial legal único ante RENAPO.
  - **RFC**: Identificador fiscal único ante el SAT.
  - **Número de Cuenta**: Identificador bancario fijo asignado al producto.
- **Campos Modificables:**
  - Nombres y apellidos (corrección ortográfica).
  - Teléfono móvil y alternativo.
  - Correo electrónico (si se cambia, el sistema valida que no pertenezca a otro cliente y actualiza sincrónicamente el correo del usuario de acceso).
  - Ocupación, empresa e ingresos mensuales.
  - Domicilio completo (calle, número ext/int, colonia, municipio, estado, CP).

---

## 3. Baja Lógica en Cascada (Soft Delete)

Por normativas financieras y de auditoría contable, **está estrictamente prohibido el borrado físico (`DELETE`)** de registros de clientes y movimientos bancarios.

### Comportamiento de la Baja Lógica:

1. El cliente se marca como inactivo: `cliente.activo = false`.
2. **Cascada hacia Cuentas:** Se recorren todas las cuentas asociadas y se marcan como inactivas: `cuenta.activo = false` y `cuenta.estatus = 'INACTIVA'`.
3. **Cascada hacia Usuario:** Si el cliente tiene credenciales de acceso asociadas, el usuario se desactiva inmediatamente: `usuario.activo = false`. Esto impide cualquier autenticación futura.
4. **Protección de reactivación:** La capa de servicios prohíbe crear nuevas cuentas o reactivar cuentas si el cliente se encuentra inactivo.
