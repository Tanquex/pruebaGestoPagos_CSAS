package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de pago de servicios o recarga de tiempo aire hacia GestoPago")
public class PagoServicioRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Size(min = 16, max = 16, message = "El número de cuenta debe contener exactamente 16 dígitos")
    @Schema(description = "Número de cuenta bancaria del cliente emisor (16 dígitos)", example = "4152849102938475")
    private String numCuenta;

    @NotNull(message = "El ID de servicio es obligatorio")
    @Schema(description = "ID del servicio clasificador (ej. 71 para Amazon, 50 para CFE)", example = "71")
    private Integer idServicio;

    @NotNull(message = "El ID de producto es obligatorio")
    @Schema(description = "ID del producto específico en el catálogo", example = "200")
    private Integer idProducto;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    @Schema(description = "Monto a pagar o recargar", example = "100.00")
    private BigDecimal precio;

    @Schema(description = "Identificador del distribuidor asignado", example = "83")
    private Integer distribuidor;

    @NotBlank(message = "El teléfono o referencia es obligatorio")
    @Schema(description = "Número telefónico (10 dígitos para tiempo aire) o número de referencia/recibo", example = "5512345678")
    private String telefonoReferencia;

    @NotBlank(message = "La bandera es obligatoria")
    @Schema(description = "Indicador de operación: SIN_REFERENCIA (Tiempo Aire) o CON_REFERENCIA (Pago Servicios)", example = "SIN_REFERENCIA")
    private String bandera;

    @NotBlank(message = "El PIN o contraseña de autorización es obligatorio")
    @Schema(description = "PIN o credencial de seguridad del cliente para autorizar el cargo", example = "Password123*")
    private String pin;

    @NotBlank(message = "El folio de idempotencia (randomString) es obligatorio")
    @Size(min = 8, max = 64, message = "El randomString debe tener entre 8 y 64 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "El randomString solo admite letras, números y guiones")
    @Schema(description = "Llave de idempotencia generada por el cliente (aleatoria y única por operación). Reintentar con el mismo valor nunca duplica el cargo", example = "TX-998877AABB")
    private String randomString;
}
