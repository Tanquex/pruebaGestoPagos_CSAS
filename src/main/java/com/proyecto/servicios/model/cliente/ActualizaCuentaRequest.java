package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para actualizar el estatus de una cuenta bancaria")
public class ActualizaCuentaRequest {

    @NotBlank(message = "El estatus de la cuenta es obligatorio")
    @Pattern(regexp = "^(?i)(ACTIVA|INACTIVA|BLOQUEADA|CANCELADA)$", message = "El estatus debe ser ACTIVA, INACTIVA, BLOQUEADA o CANCELADA")
    @Schema(example = "BLOQUEADA")
    private String estatus;
}
