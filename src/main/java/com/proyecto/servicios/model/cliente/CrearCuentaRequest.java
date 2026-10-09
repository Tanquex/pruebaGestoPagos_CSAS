package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud para crear una nueva cuenta bancaria para un cliente existente")
public class CrearCuentaRequest {

    @NotNull(message = "El identificador del cliente es obligatorio")
    @Schema(example = "1")
    private Long clienteId;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Schema(example = "500.00")
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;
}
