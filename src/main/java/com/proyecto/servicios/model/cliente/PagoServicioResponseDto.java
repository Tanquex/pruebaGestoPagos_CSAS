package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta del procesamiento de pago de servicio hacia GestoPago")
public class PagoServicioResponseDto {

    @Schema(description = "ID de auditoría interna de la transacción", example = "1")
    private Long idTransaccion;

    @Schema(description = "Folio único de transacción generado para idempotencia", example = "TX-839210AB48")
    private String randomString;

    @Schema(description = "Número de cuenta bancaria con dígitos centrales enmascarados", example = "4152********8475")
    private String numCuenta;

    @Schema(description = "ID del servicio", example = "71")
    private Integer idServicio;

    @Schema(description = "ID del producto", example = "200")
    private Integer idProducto;

    @Schema(description = "Nombre comercial del servicio", example = "Amazon")
    private String nombreServicio;

    @Schema(description = "Nombre comercial del producto", example = "Amazon $100")
    private String nombreProducto;

    @Schema(description = "Monto pagado", example = "100.00")
    private BigDecimal precio;

    @Schema(description = "Saldo restante en la cuenta del cliente tras el cobro", example = "2400.00")
    private BigDecimal saldoRestante;

    @Schema(description = "Teléfono o referencia enmascarada", example = "55****78")
    private String telefonoReferencia;

    @Schema(description = "Bandera de operación", example = "SIN_REFERENCIA")
    private String bandera;

    @Schema(description = "Estatus final de la transacción: EXITOSA, RECHAZADA o EN_REVISION", example = "EXITOSA")
    private String estatus;

    @Schema(description = "Código de respuesta retornado por GestoPago (01 = Éxito)", example = "01")
    private String codigoProveedor;

    @Schema(description = "Mensaje descriptivo retornado por GestoPago", example = "Operacion realizada con exito")
    private String mensajeProveedor;

    @Schema(description = "Folio de autorización del proveedor externo", example = "FOL-849102")
    private String folioProveedor;

    @Schema(description = "Número de autorización de la transacción", example = "AUTH-73910")
    private String autorizacionProveedor;

    @Schema(description = "Fecha y hora del registro", example = "2026-10-27T16:20:00")
    private LocalDateTime fechaTransaccion;
}
