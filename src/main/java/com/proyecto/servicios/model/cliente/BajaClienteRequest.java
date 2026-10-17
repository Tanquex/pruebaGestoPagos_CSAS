package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para baja lógica de cliente en cascada")
public class BajaClienteRequest {

    @NotNull(message = "El ID del cliente es obligatorio para procesar la baja lógica")
    @Schema(description = "Identificador único del cliente a desactivar", example = "1")
    private Long id;
}
