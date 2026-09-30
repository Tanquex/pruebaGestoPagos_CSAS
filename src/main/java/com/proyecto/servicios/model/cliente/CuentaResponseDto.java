package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.entity.cliente.Cuenta;
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
@Schema(description = "Datos de respuesta para cuentas bancarias")
public class CuentaResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "10")
    private Long clienteId;

    @Schema(example = "0123456789012345")
    private String numeroCuenta;

    @Schema(example = "1000.00")
    private BigDecimal saldo;

    @Schema(example = "ACTIVA")
    private String estatus;

    @Schema(example = "true")
    private Boolean activo;

    private LocalDateTime fechaCreacion;

    public static CuentaResponseDto fromEntity(Cuenta entity) {
        if (entity == null) return null;
        return CuentaResponseDto.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente() != null ? entity.getCliente().getId() : null)
                .numeroCuenta(entity.getNumeroCuenta())
                .saldo(entity.getSaldo())
                .estatus(entity.getEstatus())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
