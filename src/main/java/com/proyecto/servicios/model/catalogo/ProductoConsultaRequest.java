package com.proyecto.servicios.model.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios de consulta dinámica para catálogo de productos en MongoDB")
public class ProductoConsultaRequest {

    @Schema(description = "ID del producto específico", example = "101")
    private Integer idProducto;

    @Schema(description = "ID del servicio clasificador (ej. luz, agua, telefonía)", example = "1")
    private Integer idServicio;

    @Schema(description = "Texto libre para buscar por nombre de producto", example = "Telcel")
    private String buscar;
}
