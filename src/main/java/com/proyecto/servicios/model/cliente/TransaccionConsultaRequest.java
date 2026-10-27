package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios dinámicos de consulta y conciliación para transacciones de pago")
public class TransaccionConsultaRequest {

    @Schema(description = "ID único interno de la transacción", example = "1")
    private Long id;

    @Schema(description = "Número de cuenta bancaria asociada", example = "4152849102938475")
    private String numCuenta;

    @Schema(description = "Folio cliente / randomString de la transacción", example = "TX-839210AB48")
    private String randomString;

    @Schema(description = "ID del servicio clasificador", example = "71")
    private Integer idServicio;

    @Schema(description = "ID del producto específico", example = "200")
    private Integer idProducto;

    @Schema(description = "Filtrar por estatus (EXITOSA, RECHAZADA, EN_REVISION, PENDIENTE)", example = "EXITOSA")
    private String estatus;

    @Schema(description = "Fecha inicial del rango de búsqueda", example = "2026-10-01T00:00:00")
    private LocalDateTime fechaInicio;

    @Schema(description = "Fecha final del rango de búsqueda", example = "2026-10-31T23:59:59")
    private LocalDateTime fechaFin;

    @Schema(description = "Número de página (inicia en 0)", example = "0")
    private Integer pagina;

    @Schema(description = "Tamaño de la página (por defecto 20)", example = "20")
    private Integer tamanoPagina;

    @Schema(description = "Campo por el cual ordenar (ej. id, fechaCreacion, precio)", example = "fechaCreacion")
    private String ordenarPor;

    @Schema(description = "Dirección del ordenamiento: ASC o DESC", example = "DESC")
    private String direccionOrden;
}
