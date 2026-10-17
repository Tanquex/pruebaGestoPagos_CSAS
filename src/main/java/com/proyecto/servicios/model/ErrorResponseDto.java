package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estructura estándar uniforme para errores de la API")
public class ErrorResponseDto {

    @Schema(description = "Código de estado HTTP", example = "400")
    private Integer codigo;

    @Schema(description = "Descripción del error", example = "Error de validación en los datos enviados")
    private String mensaje;

    @Schema(description = "Detalle de campos que fallaron la validación")
    private Map<String, String> errores;

    @Builder.Default
    @Schema(description = "Marca de tiempo del error", example = "2026-10-17T01:50:00")
    private LocalDateTime timestamp = LocalDateTime.now();
}
