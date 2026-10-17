# Guía Integral de la Aplicación y Plan de Pruebas en Swagger

Este documento explica **completamente desde cero** qué hace la aplicación, su arquitectura, el flujo de datos, la explicación línea por línea de los componentes clave y una **guía práctica paso a paso para probar todos los endpoints en Swagger** con datos listos para copiar y pegar.

---

## 1. ¿Qué hace la aplicación? (Visión General desde Cero)

Esta aplicación es un **microservicio bancario y financiero** desarrollado con **Spring Boot 3.3.6** y **Java 21**. Su función principal es resolver el problema de **Onboarding de Clientes**: el proceso que vive un banco cuando una persona física llega a solicitar la apertura de una cuenta.

El sistema se compone de dos grandes módulos:

```
                                  ┌────────────────────────────────────────┐
                                  │      ARQUITECTURA DE LA APLICACIÓN     │
                                  └───────────────────┬────────────────────┘
                                                      │
                       ┌──────────────────────────────┴──────────────────────────────┐
                       ▼                                                             ▼
       ┌──────────────────────────────┐                             ┌──────────────────────────────┐
       │   MÓDULO 1: BANCA DIGITAL    │                             │      MÓDULO 2: CATÁLOGO      │
       │     (PostgreSQL Local)       │                             │       (MongoDB Local)        │
       ├──────────────────────────────┤                             ├──────────────────────────────┤
       │ • Onboarding de Clientes     │                             │ • Sincronización XML con     │
       │ • Domicilio del Cliente      │                             │   proveedor GestoPago        │
       │ • Creación Cuenta Bancaria   │                             │ • Catálogo de servicios      │
       │ • Creación Usuario y BCrypt  │                             │   (luz, agua, recargas)      │
       │ • Autenticación JWT          │                             │ • Consultas NoSQL            │
       │ • Consultas masivas (100k)   │                             └──────────────────────────────┘
       │ • Baja lógica en cascada     │
       └──────────────────────────────┘
```

---

## 2. Flujo de Datos y Código Explicado Detalladamente

### 2.1. El Flujo de una Petición (Capas de Software)

Cuando realizas una llamada HTTP (por ejemplo, desde Swagger):

1. **Controlador (`Controller`):** Recibe la petición HTTP, desempaqueta el JSON en un DTO y aplica las validaciones con `@Valid`.
2. **Servicio (`Service` / `ServiceImpl`):** Contiene las reglas del banco (¿Es mayor de 18 años? ¿Ya existe la CURP? ¿Cómo se encripta la contraseña?). Maneja transacciones con `@Transactional`.
3. **Repositorio (`Repository`):** Traduce las operaciones de Java a consultas SQL nativas optimizadas mediante Spring Data JPA.
4. **Base de Datos (`PostgreSQL`):** Guarda la información garantizando que los datos no se corrompan gracias a llaves foráneas e índices B-Tree.

---

### 2.2. Explicación de los Componentes Clave del Código

#### A. Base de Datos y Migración Flyway (`V3__onboarding_clientes.sql`)
- **`domicilios`:** Almacena dirección física (`calle`, `numero_exterior`, `codigo_postal`, etc.).
- **`clientes`:** Almacena los datos personales del cliente (`nombre`, `fecha_nacimiento`, `curp`, `rfc`, etc.).
- **`cuentas`:** Almacena las cuentas bancarias (`numero_cuenta`, `saldo`, `estatus`). Cada cuenta apunta a un cliente (`cliente_id`).
- **`usuarios`:** Almacena las credenciales de acceso (`correo`, `password_hash`).
- **Índices B-Tree:** Se crearon 14 índices sobre campos clave (`curp`, `rfc`, `correo`, `numero_cuenta`, `activo`). Gracias a esto, cuando el profesor pruebe 100,000 registros, las búsquedas tomarán milisegundos ($O(\log N)$) en lugar de saturar la memoria.

#### B. Generador de Cuentas Único (`GeneradorCuentaUtil.java`)
- Genera un número de cuenta bancaria de **16 dígitos** con formato estándar de tarjeta: prefijo BIN `4152` seguido de 12 dígitos aleatorios (`ThreadLocalRandom`).
- Verifica contra `cuentaRepository.existsByNumeroCuenta` para garantizar matemáticamente cero colisiones.

#### C. Seguridad de Contraseñas (`BCryptConfig.java`)
- Nunca se guardan contraseñas en texto plano. Se utiliza **BCrypt** con factor de coste `10` ($2^{10} = 1024$ rondas de hashing).
- Cada contraseña incluye un *salt* criptográfico de 16 bytes que previene ataques de fuerza bruta y tablas rainbow.

#### D. Autenticación y Tokens (`AuthServiceImpl.java`)
- Cuando el usuario manda su correo y contraseña a `POST /auth/login`, el servicio busca al usuario, comprueba que esté activo (`activo = true`) y valida la contraseña con `passwordEncoder.matches()`.
- Si es correcta, genera un token **JWT (JSON Web Token)** firmado con HMAC-SHA256 con tiempo de expiración de 24 horas.

#### E. Paginación sin Fallos (`@ParameterObject`)
- En Spring Data, cuando consultas listas masivas se usa `Pageable pageable` (`page`, `size`, `sort`).
- Al agregar `@ParameterObject` en los controladores, Swagger muestra tres campos opcionales independientes y evita enviar valores inválidos como `sort=string` que rompían la aplicación con error 500.

#### F. Manejador Global de Errores (`GlobalExceptionHandler.java`)
- Atrapa cualquier error y lo transforma en un mensaje amigable con el código HTTP correspondiente:
  - `400 Bad Request`: Formato de JSON o datos inválidos (ej. teléfono de 8 dígitos en vez de 10).
  - `401 Unauthorized`: Contraseña incorrecta o usuario inactivo.
  - `404 Not Found`: Cliente o cuenta inexistente.
  - `409 Conflict`: CURP, RFC o Correo ya registrados previamente.
  - `422 Unprocessable Entity`: Reglas de negocio no cumplidas (ej. cliente menor de edad).

---

## 3. Plan de Pruebas Paso a Paso en Swagger

Abre en tu navegador:
👉 **`http://localhost:8081/swagger-ui.html`**

Sigue exactamente este orden cronológico para entender cómo fluye toda la información:

---

### 🟢 PASO 1: Onboarding Completo de un Cliente (Alta Integral)
En este paso se crea el cliente, se le asigna automáticamente un domicilio, una cuenta bancaria con saldo y un usuario con contraseña encriptada.

1. Ve a la sección **Clientes** ➔ **`POST /clientes`**.
2. Dale clic a **Try it out**.
3. Pega el siguiente JSON en el cuadro de texto:
```json
{
  "nombre": "Carlos",
  "segundoNombre": "Eduardo",
  "apellidoPaterno": "López",
  "apellidoMaterno": "García",
  "fechaNacimiento": "1994-08-20",
  "curp": "LOGC940820HDFRRN01",
  "rfc": "LOGC9408201A2",
  "sexo": "M",
  "nacionalidad": "Mexicana",
  "estadoCivil": "SOLTERO",
  "correo": "carlos.lopez@example.com",
  "telefonoMovil": "5512345678",
  "telefonoAlternativo": "5587654321",
  "ocupacion": "Desarrollador Java",
  "empresa": "Tech Solutions México",
  "ingresoMensual": 35000.00,
  "saldoInicial": 2500.00,
  "password": "PasswordSeguro123",
  "domicilio": {
    "calle": "Av. Paseo de la Reforma",
    "numeroExterior": "222",
    "numeroInterior": "Piso 8",
    "colonia": "Juárez",
    "municipio": "Cuauhtémoc",
    "estado": "CDMX",
    "codigoPostal": "06600",
    "pais": "México"
  }
}
```
4. Haz clic en **Execute**.
5. **Resultado esperado (HTTP 201 Created):**
   - Recibirás un JSON con `id: 1` (o el siguiente ID disponible).
   - En la lista `cuentas` verás una cuenta con número que inicia con `4152...` y saldo de `2500.00`.
   - En `usuario` verás el correo `carlos.lopez@example.com`.
   - **Copia el número de cuenta generado** (ejemplo: `4152123456789012`) para usarlo en los siguientes pasos.

---

### 🟢 PASO 2: Inicio de Sesión y Obtención del Token JWT
Ahora vamos a autenticarnos con el usuario que se creó en el Paso 1.

1. Ve a la sección **Autenticación** ➔ **`POST /auth/login`**.
2. Dale clic a **Try it out**.
3. Pega las credenciales de Carlos:
```json
{
  "correo": "carlos.lopez@example.com",
  "password": "PasswordSeguro123"
}
```
4. Haz clic en **Execute**.
5. **Resultado esperado (HTTP 200 OK):**
   ```json
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "tipoToken": "Bearer",
     "correo": "carlos.lopez@example.com",
     "clienteId": 1,
     "mensaje": "Autenticación exitosa"
   }
   ```
6. **Activar el Token en Swagger:**
   - Copia todo el texto de `"token"`.
   - Sube al inicio de Swagger y haz clic en el botón verde **Authorize**.
   - Pega el token en el campo `Value:` y haz clic en **Authorize** y luego **Close**.

---

### 🟢 PASO 3: Consultas Dinámicas de Clientes (Único Endpoint con Criteria API)

El sistema implementa un único endpoint dinámico combinable mediante **`POST /clientes/consultar`** (recibiendo los filtros en el cuerpo JSON, sin path variables ni query params):

1. **Listado general de clientes (sin filtros):**
   - En **`POST /clientes/consultar`**, envía cuerpo vacío `{}` y clic en **Execute**.
   - **Resultado:** Código `200 OK` paginado con todos los clientes.
2. **Consulta por ID:**
   - Envía: `{"id": 1}`.
   - **Resultado:** Código `200 OK` con los datos de Carlos López.
3. **Consulta por CURP (B-Tree indexado):**
   - Envía: `{"curp": "LOGC940820HDFRRN01"}`.
   - **Resultado:** Código `200 OK` encontrando al cliente en milisegundos gracias al índice B-Tree.
4. **Consulta por RFC:**
   - Envía: `{"rfc": "LOGC9408201A2"}`.
   - **Resultado:** Código `200 OK`.
5. **Búsqueda por coincidencia de nombre o apellido:**
   - Envía: `{"nombre": "carlos"}`.
   - **Resultado:** Código `200 OK` devolviendo coincidencias parciales con `LIKE %carlos%`.
6. **Búsqueda por número de cuenta bancaria:**
   - Envía: `{"numeroCuenta": "4152..."}` (pega el número de cuenta generado en el paso 1).
   - **Resultado:** Código `200 OK` uniendo el cliente con su cuenta mediante JPA Criteria Join.
7. **Filtrado por estatus activo y paginación:**
   - Envía: `{"activo": true, "pagina": 0, "tamanoPagina": 10, "ordenarPor": "id", "direccionOrden": "ASC"}`.
   - **Resultado:** Código `200 OK`.

---

### 🟢 PASO 4: Operaciones y Consultas de Cuentas Bancarias

1. **Consulta dinámica de cuenta y saldo:**
   - Ve a **`POST /cuentas/consultar`** y envía:
     ```json
     {
       "numeroCuenta": "4152849102938475"
     }
     ```
   - **Resultado:** Código `200 OK` con el detalle de la cuenta, saldo y estatus `ACTIVA`.
2. **Crear una segunda cuenta para el cliente:**
   - Ve a **`POST /cuentas`** y envía:
     ```json
     {
       "clienteId": 1,
       "saldoInicial": 500.00
     }
     ```
   - **Resultado:** Código `201 Created` con una segunda cuenta bancaria independiente.
3. **Listar todas las cuentas del cliente:**
   - En **`POST /cuentas/consultar`**, envía:
     ```json
     {
       "clienteId": 1
     }
     ```
   - **Resultado:** Código `200 OK` mostrando las dos cuentas asociadas a Carlos.
4. **Actualizar estatus operativo de una cuenta (Bloqueo):**
   - Ve a **`PATCH /cuentas/estatus`** y envía:
     ```json
     {
       "numeroCuenta": "4152849102938475",
       "estatus": "BLOQUEADA"
     }
     ```
   - **Resultado:** Código `200 OK` con `estatus: "BLOQUEADA"` y `activo: false`.

---

### 🟢 PASO 5: Consultas de Usuarios

1. **Listado paginado de usuarios:**
   - Ve a **`POST /usuarios/consultar`** y envía `{}`.
   - **Resultado:** Código `200 OK` listando los usuarios registrados (con su ID y correo, sin exponer el password).
2. **Filtro por correo o clienteId:**
   - Envía: `{"correo": "carlos"}` o `{"clienteId": 1}`.
   - **Resultado:** Código `200 OK` con la coincidencia correspondiente.

---

### 🟢 PASO 6: Actualización Parcial de Cliente (`PATCH`)
Regla del banco: Se pueden actualizar datos personales y domicilio, pero **está estrictamente prohibido alterar la CURP y el RFC**. El ID del cliente se envía en el cuerpo JSON:

1. Ve a **`PATCH /clientes`**.
2. Pega este JSON para cambiar el teléfono y la empresa:
```json
{
  "id": 1,
  "telefonoMovil": "5599887766",
  "empresa": "Google Cloud Partner",
  "ingresoMensual": 45000.00
}
```
3. Clic en **Execute**.
4. **Resultado esperado (HTTP 200 OK):**
   - Verás el teléfono actualizado a `5599887766` y el ingreso a `45000.00`.
   - La CURP (`LOGC940820HDFRRN01`) y el RFC permanecen exactamente intactos por inmutabilidad de identidad.

---

### 🟢 PASO 7: Baja Lógica en Cascada (`DELETE`)
Regla financiera: **Nunca se borra físicamente (`DELETE FROM`) la información contable**. Se desactiva el cliente y en cascada se desactivan sus cuentas y su usuario. El ID se recibe en el cuerpo JSON:

1. Ve a **`DELETE /clientes`**.
2. Envía en el Request Body:
```json
{
  "id": 1
}
```
3. Clic en **Execute**.
4. **Resultado esperado:** Código **`204 No Content`** (operación exitosa sin cuerpo).
5. **Comprobación de la Cascada:**
   - Ve a **`POST /clientes/consultar`** con `{"id": 1}`: Verás `"activo": false`.
   - Ve a **`POST /cuentas/consultar`** con `{"clienteId": 1}`: Verás que todas sus cuentas cambiaron a `"estatus": "INACTIVA"` y `"activo": false`.
   - Ve a **`POST /auth/login`** e intenta iniciar sesión con las credenciales de Carlos:
     **Resultado:** Código **`401 Unauthorized`** con el mensaje `"Credenciales inválidas o usuario inactivo."`. ¡El usuario quedó inhabilitado automáticamente!

---

### 🔴 PASO 8: Pruebas de Validaciones y Casos de Error (Lo que evalúa el Profesor)

Estas pruebas demuestran la solidez de las validaciones de negocio:

1. **Prueba de Menor de Edad (HTTP 422):**
   - En `POST /clientes`, intenta registrar un cliente con fecha de nacimiento reciente (ej. `"fechaNacimiento": "2015-05-10"`).
   - **Resultado:** Código `422 Unprocessable Entity` con el mensaje: *"El cliente debe ser mayor de edad (18 años o más)"*.
2. **Prueba de CURP Duplicada (HTTP 409):**
   - En `POST /clientes`, intenta registrar otro cliente usando la misma CURP `LOGC940820HDFRRN01`.
   - **Resultado:** Código `409 Conflict` con el mensaje: *"Ya existe un cliente registrado con la CURP..."*.
3. **Prueba de Formato Inválido (HTTP 400):**
   - En `POST /clientes`, envía un teléfono de 7 dígitos o una CURP de 10 letras.
   - **Resultado:** Código `400 Bad Request` indicando exactamente qué campo no cumple con el formato requerido.
4. **Prueba de Contraseña Incorrecta (HTTP 401):**
   - En `POST /auth/login`, pon una contraseña errónea.
   - **Resultado:** Código `401 Unauthorized` con el mensaje: *"Credenciales inválidas o usuario inactivo."*.
