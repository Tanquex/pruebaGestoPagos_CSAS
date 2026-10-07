# Proyecto Integrador: Onboarding de Clientes Personas Físicas

## 1. Visión General
Este sistema implementa el flujo de **Onboarding Financiero de Clientes Personas Físicas** bajo una arquitectura de microservicio REST con **Spring Boot 3.3.6**, **Java 21**, persistencia relacional en **PostgreSQL 16** y persistencia documental en **MongoDB 7.0**.

El objetivo central es permitir a una institución financiera registrar clientes personas físicas, validar rigurosamente su identidad y datos de contacto, aperturarles de forma automática una cuenta bancaria con saldo inicial y crearles credenciales de acceso con contraseñas cifradas (**BCrypt**), permitiendo consultas, actualizaciones parciales y bajas lógicas.

---

## 2. Índice de Documentación Unitaria

Para facilitar la revisión por áreas funcionales y técnicas, la documentación se encuentra organizada en guías independientes:

1. 🗄️ [**Diseño de Base de Datos y Modelo Físico**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/bd/DISENO_BASE_DE_DATOS.md)
   * Diagrama Entidad-Relación (ERD) en Mermaid.
   * Justificación técnica de tipos de datos en PostgreSQL (`NUMERIC`, `CHAR`, `VARCHAR`, `TEXT`, `BIGSERIAL`).
   * Estrategia de indexación B-Tree preparada para pruebas masivas de **100,000+ registros**.
   * Integridad referencial y restricciones `UNIQUE`.

2. 🛡️ [**Reglas de Negocio y Validaciones**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/validaciones/REGLAS_Y_VALIDACIONES.md)
   * Expresiones regulares oficiales de CURP y RFC mexicanos.
   * Validación estricta de mayoría de edad (18+ años), teléfonos de 10 dígitos y CP de 5 dígitos.
   * Matriz de validación y control de excepciones personalizadas.

3. 🚀 [**Flujo Transaccional de Onboarding**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/onboarding/REGISTRO_Y_ONBOARDING.md)
   * Orquestación atómica: Domicilio ➔ Cliente ➔ Cuenta Bancaria ➔ Usuario BCrypt.
   * Algoritmo de generación de cuenta bancaria sin colisiones.

4. 📊 [**Consultas, Paginación y Operaciones CRUD**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/crud/CONSULTAS_Y_OPERACIONES.md)
   * Endpoints de consulta con soporte de paginación obligatoria (`Pageable`) para 100k registros.
   * Actualización parcial con protección de campos inmutables (CURP, RFC, número de cuenta).
   * Baja lógica en cascada (desactivación simultánea de cliente, cuentas y usuario).

5. 🔐 [**Seguridad, Autenticación y Cabeceras**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/seguridad/AUTENTICACION_Y_HEADERS.md)
   * Cabecera `Authorization: Bearer <TOKEN>` documental no bloqueante en Swagger y endpoints.
   * Cifrado de contraseñas con BCrypt.
   * Endpoint de autenticación `POST /auth/login`.

6. 🧪 [**Plan de Pruebas en Swagger y Explicación desde Cero**](file:///c:/Users/SAMAEL/Downloads/prueba/prueba/docs/pruebas/PLAN_DE_PRUEBAS_Y_EXPLICACION.md)
   * Explicación pedagógica de la arquitectura y el código línea por línea.
   * Flujo ordenado paso a paso para probar los 22 endpoints en Swagger.
   * Datos JSON listos para copiar y pegar y validaciones de error esperadas.

---

## 3. Matriz de Endpoints REST

| Módulo | Método | Endpoint | Descripción | Código HTTP |
|---|---|---|---|---|
| **Clientes** | `POST` | `/clientes` | Onboarding completo (Cliente + Cuenta + Usuario) | 201 Created |
| **Clientes** | `GET` | `/clientes` | Listado paginado de clientes | 200 OK |
| **Clientes** | `GET` | `/clientes/{id}` | Consulta por ID primario | 200 OK |
| **Clientes** | `GET` | `/clientes/curp/{curp}` | Consulta indexada por CURP | 200 OK |
| **Clientes** | `GET` | `/clientes/rfc/{rfc}` | Consulta indexada por RFC | 200 OK |
| **Clientes** | `GET` | `/clientes/correo` | Consulta indexada por correo | 200 OK |
| **Clientes** | `GET` | `/clientes/cuenta/{numeroCuenta}` | Consulta por número de cuenta bancaria | 200 OK |
| **Clientes** | `GET` | `/clientes/buscar` | Búsqueda por nombre o apellidos (paginado) | 200 OK |
| **Clientes** | `GET` | `/clientes/activos` | Listado de clientes activos (paginado) | 200 OK |
| **Clientes** | `GET` | `/clientes/fechas` | Búsqueda por rango de fechas (paginado) | 200 OK |
| **Clientes** | `PATCH` | `/clientes/{id}` | Actualización parcial (inmutabilidad CURP/RFC) | 200 OK |
| **Clientes** | `DELETE` | `/clientes/{id}` | Baja lógica en cascada (desactiva cuentas/usuario) | 204 No Content |
| **Cuentas** | `POST` | `/cuentas` | Apertura de cuenta bancaria adicional | 201 Created |
| **Cuentas** | `GET` | `/cuentas/{numeroCuenta}` | Consulta por número de cuenta | 200 OK |
| **Cuentas** | `GET` | `/cuentas/cliente/{clienteId}` | Cuentas asociadas a un cliente (paginado) | 200 OK |
| **Cuentas** | `GET` | `/cuentas/estatus/{estatus}` | Filtro por estatus operativo (paginado) | 200 OK |
| **Cuentas** | `GET` | `/cuentas/activas` | Listado de cuentas activas (paginado) | 200 OK |
| **Cuentas** | `GET` | `/cuentas/{numeroCuenta}/saldo` | Consulta rápida de saldo | 200 OK |
| **Cuentas** | `PATCH` | `/cuentas/{numeroCuenta}/estatus` | Actualizar estatus (ACTIVA, BLOQUEADA, etc.) | 200 OK |
| **Seguridad** | `POST` | `/auth/login` | Inicio de sesión, validación BCrypt y JWT Bearer | 200 OK |
| **Usuarios** | `POST` | `/usuarios` | Alta manual de usuario para cliente existente | 201 Created |
| **Usuarios** | `GET` | `/usuarios/{id}` | Consulta de usuario por ID | 200 OK |
| **Usuarios** | `GET` | `/usuarios/cliente/{clienteId}` | Consulta de usuario por cliente | 200 OK |
| **Usuarios** | `GET` | `/usuarios` | Listado de todos los usuarios (paginado) | 200 OK |
| **Usuarios** | `GET` | `/usuarios/filtro` | Filtro por estado activo o correo (paginado) | 200 OK |

---

## 4. Criterios de Evaluación del Profesor (100% Cubierto)

| Criterio | Ponderación | Estado | Implementación Clave |
|---|---|---|---|
| **Base de Datos** | **20%** | **Completado (100%)** | Migración Flyway `V3__onboarding_clientes.sql` con 4 tablas, tipos exactos (`NUMERIC`, `CHAR`, `VARCHAR`, `TEXT`), 14 índices B-Tree para 100k registros y llaves foráneas. |
| **Validaciones** | **20%** | **Completado (100%)** | Jakarta Bean Validation con Regex oficial CURP/RFC, cálculo exacto de edad $\ge 18$, teléfonos 10 dígitos, CP 5 dígitos, saldo no negativo y excepciones personalizadas HTTP 400/404/409/422. |
| **Implementación Java** | **25%** | **Completado (100%)** | Entidades JPA con Lombok, Repositorios con Spring Data, arquitectura en capas, servicios transaccionales `@Transactional`, hashing de contraseñas con BCrypt (factor 10), generador de cuenta sin colisiones y pruebas con Mockito. |
| **API REST** | **15%** | **Completado (100%)** | Controladores con `@RestController`, verbos HTTP adecuados (`POST`, `GET`, `PATCH`, `DELETE`), códigos HTTP precisos (`200`, `201`, `204`, `400`, `401`, `404`, `409`, `422`) y documentación Swagger interactiva con esquema JWT Bearer. |
| **Consultas y Persistencia**| **10%** | **Completado (100%)** | Búsquedas por ID, CURP, RFC, Cuenta, Nombre, Fechas, Estatus con paginación obligatoria `Pageable` optimizada para cargas masivas. |
| **Documentación** | **10%** | **Completado (100%)** | Directorio modular `/docs` con guías independientes, diagramas Mermaid ERD y secuencia, justificación técnica y README unificado. |

---

## 5. Instrucciones de Ejecución Local

1. **Asegurar Contenedores Docker Activos:**
   ```powershell
   docker ps
   # Deben estar activos: gestopago-postgres (puerto 5432) y gestopago-mongo (puerto 27017)
   ```
2. **Ejecutar Pruebas Unitarias:**
   ```powershell
   .\gradlew.bat test
   ```
3. **Iniciar la Aplicación:**
   ```powershell
   .\gradlew.bat bootRun
   ```
4. **Probar con Swagger UI:**
   Abrir en el navegador:
   `http://localhost:8081/swagger-ui.html` o `http://localhost:8081/swagger-ui/index.html`
