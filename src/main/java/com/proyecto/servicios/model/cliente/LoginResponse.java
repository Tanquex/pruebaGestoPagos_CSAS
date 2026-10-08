package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de autenticación exitosa")
public class LoginResponse {

    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(example = "Bearer")
    @Builder.Default
    private String tipoToken = "Bearer";

    @Schema(example = "juan.perez@example.com")
    private String correo;

    @Schema(example = "1")
    private Long clienteId;

    @Schema(example = "Autenticación exitosa")
    private String mensaje;
}
