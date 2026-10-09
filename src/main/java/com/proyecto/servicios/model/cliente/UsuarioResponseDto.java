package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.entity.cliente.Usuario;
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
@Schema(description = "Datos de respuesta para usuarios del sistema")
public class UsuarioResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "10")
    private Long clienteId;

    @Schema(example = "juan.perez@example.com")
    private String correo;

    @Schema(example = "true")
    private Boolean activo;

    private LocalDateTime fechaCreacion;

    public static UsuarioResponseDto fromEntity(Usuario entity) {
        if (entity == null) return null;
        return UsuarioResponseDto.builder()
                .id(entity.getId())
                .clienteId(entity.getCliente() != null ? entity.getCliente().getId() : null)
                .correo(entity.getCorreo())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .build();
    }
}
