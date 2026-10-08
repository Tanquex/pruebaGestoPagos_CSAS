# Documentación Unitaria: Reglas de Negocio y Validaciones

Esta guía detalla las validaciones de entrada, expresiones regulares oficiales mexicanas y reglas de negocio aplicadas para el onboarding de clientes y cuentas bancarias.

---

## 1. Matriz de Validaciones por Campo

| Campo | Obligatorio | Tipo / Longitud | Expresión Regular / Regla | Mensaje de Error |
| :--- | :--- | :--- | :--- | :--- |
| **Nombre** | Sí | 2 - 50 caracteres | `^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$` | Solo letras y espacios permitidos |
| **Segundo Nombre** | No | Máx 50 caracteres | `^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]*$` | Solo letras y espacios si se ingresa |
| **Apellido Paterno** | Sí | 2 - 50 caracteres | `^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$` | Solo letras y espacios permitidos |
| **Apellido Materno** | Sí | 2 - 50 caracteres | `^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$` | Solo letras y espacios permitidos |
| **Fecha Nacimiento** | Sí | `LocalDate` (`@Past`) | `Period.between(nacimiento, hoy) >= 18` | Debe ser mayor de edad (18 años o más) y no ser fecha futura |
| **CURP** | Sí | 18 caracteres exactos | `^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$` | Formato oficial RENAPO de 18 caracteres |
| **RFC** | Sí | 12 o 13 caracteres | `^[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}$` | Formato oficial SAT de 12 o 13 caracteres |
| **Correo** | Sí | Máx 100 caracteres | RFC 5322 (`@Email`) | Formato de correo válido y único |
| **Teléfono Móvil** | Sí | 10 dígitos exactos | `^\d{10}$` | Exactamente 10 dígitos numéricos |
| **Teléfono Alterno**| No | 10 dígitos | `^$|^\d{10}$` | 10 dígitos numéricos si se proporciona |
| **Código Postal** | Sí | 5 dígitos exactos | `^\d{5}$` | Exactamente 5 dígitos numéricos |
| **Ingreso Mensual** | Sí | `BigDecimal` | `> 0.00` (`@DecimalMin("0.01")`) | El ingreso mensual debe ser mayor a cero |
| **Saldo Inicial** | No | `BigDecimal` | `>= 0.00` (`@DecimalMin("0.00")`) | El saldo inicial no puede ser negativo |
| **Contraseña** | Sí | Mínimo 8 caracteres | `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&._#\-])[A-Za-z\d@$!%*?&._#\-]{8,}$` | Requiere mayúscula, minúscula, número y símbolo |

---

## 2. Detalle de Expresiones Regulares Oficiales

### CURP (Clave Única de Registro de Población)
```regex
^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]\d$
```
* `[A-Z]{4}`: 4 letras iniciales del nombre y apellidos.
* `\d{6}`: Fecha de nacimiento en formato `AAMMDD`.
* `[HM]`: Sexo (`H` para Hombre, `M` para Mujer).
* `[A-Z]{5}`: 2 letras de la entidad federativa de nacimiento + 3 consonantes internas de los apellidos y nombre.
* `[A-Z0-9]\d`: Homoclave y dígito verificador.

### RFC (Registro Federal de Contribuyentes)
```regex
^[A-ZÑ&]{3,4}\d{6}[A-Z0-9]{3}$
```
* `[A-ZÑ&]{3,4}`: 4 letras para personas físicas o 3 para personas morales.
* `\d{6}`: Fecha de nacimiento o constitución (`AAMMDD`).
* `[A-Z0-9]{3}`: Homoclave oficial asignada por el SAT.

---

## 3. Reglas de Negocio en Base de Datos y Servicio

1. **Unicidad Estricta:**
   * No pueden existir dos clientes con la misma CURP, RFC o Correo. Se valida en la capa de servicio con `existsBy...` y a nivel motor de base de datos con `CONSTRAINT UNIQUE`.
2. **Inmutabilidad de Identificadores:**
   * En las actualizaciones (`PATCH` / `PUT`), **está estrictamente prohibido alterar la CURP, el RFC o el número de cuenta**. Cualquier intento de modificación en el payload es ignorado o rechazado.
3. **Control de Saldo No Negativo:**
   * Protegido en la entidad (`@DecimalMin("0.00")`), en el DTO y en PostgreSQL con `CONSTRAINT chk_cuentas_saldo_no_negativo CHECK (saldo >= 0.00)`.
4. **Relación de Estatus y Baja Lógica:**
   * Solo los clientes activos pueden tener cuentas activas. Si un cliente pasa a `activo = false`, todas sus cuentas asociadas y su usuario pasan automáticamente a `activo = false`.

---

## 4. Respuestas de Error Estructuradas

Cuando una validación falla, el `GlobalExceptionHandler` devuelve:
```json
{
  "codigo": 400,
  "mensaje": "Error de validación en los datos enviados",
  "errores": {
    "curp": "La CURP debe tener formato oficial mexicano de 18 caracteres (RENAPO)",
    "ingresoMensual": "El ingreso mensual debe ser mayor a cero",
    "password": "La contraseña debe contener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
  }
}
```
Y ante duplicados concurrentes en pruebas masivas:
```json
{
  "codigo": 409,
  "mensaje": "Ya existe un cliente registrado con la misma CURP."
}
```
