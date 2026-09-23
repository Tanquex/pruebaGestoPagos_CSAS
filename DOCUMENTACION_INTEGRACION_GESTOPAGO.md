# Documentación Técnica: Integración de Catálogo GestoPago con JAXB y MongoDB

## 1. Resumen de la Solución
Se implementó la integración del servicio externo de catálogo de productos y servicios ofrecido por el proveedor **GestoPago** (`GET /sistema/service/getProductList.do`), siguiendo la arquitectura de capas existente en el microservicio Spring Boot.

La solución cumple con las restricciones operativas de GestoPago:
- **Restricción de frecuencia:** GestoPago limita las consultas de catálogo a un máximo de 3 diarias (recomendado 1 vez al día) para evitar baneos de IP por sospecha de denegación de servicio (DoS).
- **Almacenamiento en MongoDB:** Los productos y servicios se obtienen en formato XML, se deserializan en memoria con **JAXB** y se almacenan en una base de datos NoSQL documental (**MongoDB**). Esto permite que el frontend consulte el catálogo de manera ilimitada y ultrarrápida (en milisegundos) en formato **JSON** sin sobrecargar la API externa ni PostgreSQL.
- **Sincronización automática:** Se implementó una tarea programada (`@Scheduled`) configurada para ejecutarse todos los días a las **06:00 AM**, además de permitir sincronización manual bajo demanda.

---

## 2. Decisiones Técnicas

### 2.1 Uso de JAXB (`jakarta.xml.bind`)
* **Justificación:** La respuesta de `getProductList.do` es un documento XML que contiene atributos por elemento y etiquetas CDATA en campos como `<legend>`. JAXB es la especificación estándar de Java para mapeo objeto-XML (*Unmarshalling*), permitiendo tipado estricto mediante anotaciones (`@XmlRootElement`, `@XmlAccessorType`, `@XmlAttribute`, `@XmlElement`).

### 2.2 Uso de MongoDB en paralelo con PostgreSQL
* **PostgreSQL:** Persistencia relacional para entidades de negocio transaccionales (`personas`, `gestopago_tokens`).
* **MongoDB:** Almacén documental para catálogos semi-estructurados (`catalogo_productos`). Permite almacenar atributos variables por tipo de servicio sin requerir alteraciones de esquema relacional (DDL) ante cambios del proveedor.

### 2.3 Seguridad y Manejo de Tokens
* La petición al endpoint `getProductList.do` requiere la cabecera HTTP:
  ```http
  Authorization: Bearer <TOKEN>
  ```
* El token no está hardcodeado. El servicio lo resuelve dinámicamente desde el repositorio de tokens (`GestoPagoTokenService`), el cual mantiene los JWT renovados en PostgreSQL, contando además con una propiedad de respaldo (`gestopago.auth.token`).
* Nunca se expone el token ni credenciales sensibles en los logs ni en las trazas de error.

### 2.4 Resiliencia y Manejo de Errores
* Control de timeouts de conexión y lectura mediante excepciones específicas (`RetryableException`, `GestoPagoIntegrationException`).
* Control de respuestas HTTP no exitosas (401 Unauthorized, 403 Forbidden, 5xx) y mapeo a códigos de respuesta estandarizados vía `@RestControllerAdvice`.

---

## 3. Arquitectura de Capas Implementada

```
com.proyecto.servicios
├── client
│   └── GestoPagoCatalogoClient.java       # Cliente OpenFeign (GET getProductList.do)
├── config
│   └── ConfigMongo.java                   # Habilitación y aislamiento de @EnableMongoRepositories
├── controller
│   └── CatalogoController.java            # Endpoints REST (GET /api/v1/catalogo/productos)
├── entity/catalogo
│   └── ProductoDocument.java              # Documento MongoDB (@Document)
├── exception
│   ├── GestoPagoIntegrationException.java # Excepción personalizada de integración
│   └── GlobalExceptionHandler.java        # Manejo global de excepciones (@RestControllerAdvice)
├── model/catalogo
│   └── ProductoResponseDto.java           # DTO para respuesta limpia en JSON
├── model/gestopago/catalogo
│   ├── CatalogoXmlResponse.java           # DTO raíz JAXB (<RESPONSE>)
│   ├── MensajeXml.java                    # DTO JAXB (<MENSAJE>)
│   └── ProductoXml.java                   # DTO JAXB (<producto>)
├── repositorys/catalogo
│   └── ProductoMongoRepository.java       # Repositorio Spring Data MongoDB
├── service
│   ├── CatalogoProductoService.java       # Interfaz de servicio de negocio
│   └── Impl/CatalogoProductoServiceImpl.java # Implementación con @Scheduled (06:00 AM)
└── util
    └── JaxbXmlParser.java                 # Utilidad de deserialización JAXB
```

---

## 4. Endpoints Expuestos

| Método | Endpoint | Descripción | Formato Respuesta |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/catalogo/productos` | Consulta todos los productos en MongoDB | JSON (`200 OK`) |
| `GET` | `/api/v1/catalogo/productos?idServicio=71` | Filtra productos por ID de servicio | JSON (`200 OK`) |
| `GET` | `/api/v1/catalogo/productos?buscar=Amazon` | Búsqueda por coincidencia de nombre | JSON (`200 OK`) |
| `GET` | `/api/v1/catalogo/productos/{idProducto}` | Consulta detalle de producto por ID | JSON (`200 OK` / `404`) |
| `POST` | `/api/v1/catalogo/sincronizar` | Ejecuta sincronización manual con GestoPago | JSON (`200 OK`) |

---

## 5. Propiedades de Configuración (`application.properties`)

```properties
# Integración GestoPago Catálogo
gestopago.catalogo.url=${GESTOPAGO_CATALOGO_URL:https://gestopago.portalventas.net}
gestopago.catalogo.cron=${GESTOPAGO_CATALOGO_CRON:0 0 6 * * *}

# Conexión MongoDB
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/gestopago_catalogo}
```

---

## 6. Pruebas Unitarias Implementadas

* **`JaxbXmlParserTest`**:
  * Deserialización exitosa de XML complejo de GestoPago (atributos, etiquetas anidadas y CDATA).
  * Validación de lanzamiento de excepción ante XML vacío o malformado.
* **`CatalogoProductoServiceImplTest`**:
  * Sincronización exitosa simulando Feign Client, JAXB y persistencia en MongoDB.
  * Manejo adecuado de escenarios de error por timeout de red (`504 Gateway Timeout`).
  * Validación ante ausencia de token activo.
  * Consultas filtradas por servicio y término de búsqueda.
