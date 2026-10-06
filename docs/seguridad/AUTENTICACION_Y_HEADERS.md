# Seguridad, Autenticación y Cabeceras HTTP

Este documento detalla la arquitectura de seguridad, cifrado de contraseñas y el manejo de cabeceras de autenticación conforme a los requerimientos del proyecto integrador y las directivas solicitadas.

---

## 1. Estrategia de Autenticación y Cabeceras (Non-Blocking)

Por solicitud de la cátedra para permitir tanto pruebas manuales como pruebas masivas automatizadas (100,000+ registros) sin interrupciones ni bloqueos de acceso durante la evaluación:

1. **Cabecera `Authorization: Bearer <token>`:**
   - Se documenta en el esquema de Swagger/OpenAPI y en los controladores REST para todos los endpoints protegidos.
   - Es **no bloqueante** a nivel de filtro servlet de modo que permite consumir los servicios tanto con token como de forma directa en las pruebas de carga masiva.
2. **Generación de Token JWT:**
   - El endpoint `POST /auth/login` valida credenciales y genera un **JSON Web Token (JWT)** estándar firmado con HMAC-SHA256 (`HS256`).
   - El token contiene las siguientes declaraciones (claims):
     - `sub`: Correo electrónico del usuario.
     - `clienteId`: Identificador del cliente asociado.
     - `iat`: Timestamp de emisión (Issued At).
     - `exp`: Timestamp de expiración (24 horas de vigencia).

---

## 2. Cifrado de Contraseñas con BCrypt

Se utiliza `BCryptPasswordEncoder` de Spring Security Crypto:

- **Algoritmo:** Cifrado adaptativo basado en Blowfish.
- **Factor de Trabajo (Cost / Rounds):** `10` ($2^{10} = 1024$ iteraciones).
- **Salting Automático:** Cada hash incluye un salt aleatorio de 16 bytes que previene ataques de tablas rainbow o comparación de contraseñas idénticas.
- **Rendimiento:** El costo de 10 permite procesar cientos de registros por segundo en inserciones masivas manteniendo una robusta defensa contra ataques de fuerza bruta por diccionario.

---

## 3. Especificación del Endpoint de Autenticación

### `POST /auth/login`

**Request Body:**
```json
{
  "correo": "juan.perez@example.com",
  "password": "MiPasswordSeguro123"
}
```

**Respuesta Exitosa (HTTP 200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuLnBlcmV6QGV4YW1wbGUuY29tIiwiY2xpZW50ZUlkIjoxLCJpYXQiOjE3OTEyNDAwMDAsImV4cCI6MTc5MTMyNjQwMH0.abcdef123456...",
  "tipoToken": "Bearer",
  "correo": "juan.perez@example.com",
  "clienteId": 1,
  "mensaje": "Autenticación exitosa"
}
```

**Respuestas de Error:**
- **HTTP 401 Unauthorized (`CredencialesInvalidasException`):** Si el usuario no existe, la contraseña no coincide o el usuario se encuentra inactivo (`activo = false`).
- **HTTP 400 Bad Request:** Si el formato del correo es inválido o faltan datos obligatorios.

---

## 4. Endpoints de Consulta y Gestión de Usuarios

- `POST /usuarios`: Creación manual de credenciales para un cliente existente sin usuario.
- `GET /usuarios/{id}`: Consulta por identificador primario.
- `GET /usuarios/cliente/{clienteId}`: Consulta de usuario por cliente asociado.
- `GET /usuarios/correo`: Consulta por correo exacto.
- `GET /usuarios/filtro`: Búsqueda paginada por estado (`activo`) o coincidencia parcial de correo.
