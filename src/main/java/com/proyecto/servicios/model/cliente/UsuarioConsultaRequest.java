package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios dinámicos de consulta y paginación para usuarios")
public class UsuarioConsultaRequest {

    @Schema(description = "Filtrar por ID de usuario", example = "1")
    private Long id;

    @Schema(description = "Filtrar por ID de cliente asociado", example = "1")
    private Long clienteId;

    @Schema(description = "Filtrar por correo electrónico", example = "carlos.lopez@example.com")
    private String correo;

    @Schema(description = "Filtrar por estatus activo (true/false)", example = "true")
    private Boolean activo;

    @Min(value = 0, message = "La página debe ser mayor o igual a 0")
    @Builder.Default
    @Schema(description = "Número de página", example = "0", defaultValue = "0")
    private Integer pagina = 0;

    @Min(value = 1, message = "El tamaño de página debe ser mínimo 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    @Builder.Default
    @Schema(description = "Cantidad de registros por página", example = "20", defaultValue = "20")
    private Integer tamanoPagina = 20;

    @Builder.Default
    @Schema(description = "Campo por el cual ordenar", example = "id", defaultValue = "id")
    private String ordenarPor = "id";

    @Builder.Default
    @Schema(description = "Dirección del ordenamiento (ASC o DESC)", example = "ASC", defaultValue = "ASC")
    private String direccionOrden = "ASC";
}
