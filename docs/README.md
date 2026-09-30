# Proyecto Integrador: Onboarding de Clientes Personas Físicas

## 1. Visión General
Este sistema implementa el flujo de **Onboarding Financiero de Clientes Personas Físicas** bajo una arquitectura de microservicio REST con **Spring Boot 3.3.6**, **Java 21**, persistencia relacional en **PostgreSQL 16** y persistencia documental en **MongoDB 7.0**.

El objetivo central es permitir a una institución financiera registrar clientes, validar rigurosamente su identidad y datos de contacto, aperturarles de forma automática una cuenta bancaria con saldo inicial y crearles credenciales de acceso con contraseñas cifradas (**BCrypt**), permitiendo consultas, actualizaciones parciales y bajas lógicas.

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
   * Cabecera `Authorization: Bearer <TOKEN>` documental no bloqueante en endpoints.
   * Cifrado de contraseñas con BCrypt.
   * Endpoint de autenticación `POST /auth/login`.

---

## 3. Arquitectura Global del Sistema

```
                    ┌───────────────────────────────┐
                    │    Cliente HTTP / Swagger     │
                    └───────────────┬───────────────┘
                                    │ HTTP JSON
                                    ▼
                    ┌───────────────────────────────┐
                    │      Capa de Controladores    │
                    │ (ClienteController, Cuentas) │
                    └───────────────┬───────────────┘
                                    │ DTOs validados
                                    ▼
                    ┌───────────────────────────────┐
                    │       Capa de Servicios       │
                    │  (Reglas de negocio, @Tx)     │
                    └───────┬───────────────┬───────┘
                            │               │
                            ▼               ▼
          ┌──────────────────────────┐    ┌──────────────────────────┐
          │     Spring Data JPA      │    │    Spring Data Mongo     │
          │ (Clientes, Cuentas, BD)  │    │  (Catálogo GestoPago)    │
          └─────────────┬────────────┘    └─────────────┬────────────┘
                        │                               │
                        ▼                               ▼
          ┌──────────────────────────┐    ┌──────────────────────────┐
          │  PostgreSQL 16 (Local)   │    │     MongoDB (Local)      │
          │ - Personas Físicas       │    │ - Catálogo NoSQL 900+    │
          │ - Cuentas Bancarias      │    │   productos              │
          │ - Usuarios y Tokens      │    │                          │
          └──────────────────────────┘    └──────────────────────────┘
```
