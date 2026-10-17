package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Criterios dinámicos de búsqueda y paginación para clientes (Permite combinar filtros)")
public class ClienteConsultaRequest {

    @Schema(description = "Filtrar por ID único del cliente", example = "1")
    private Long id;

    @Schema(description = "Filtrar por CURP oficial (18 caracteres)", example = "LOGC940820HDFRRN01")
    private String curp;

    @Schema(description = "Filtrar por RFC oficial (13 caracteres)", example = "LOGC9408201A2")
    private String rfc;

    @Schema(description = "Filtrar por correo electrónico", example = "carlos.lopez@example.com")
    private String correo;

    @Schema(description = "Filtrar por número de cuenta bancaria de 16 dígitos", example = "4152849102938475")
    private String numeroCuenta;

    @Schema(description = "Búsqueda por coincidencia en nombre", example = "Carlos")
    private String nombre;

    @Schema(description = "Búsqueda por coincidencia en apellido paterno", example = "López")
    private String apellidoPaterno;

    @Schema(description = "Búsqueda por coincidencia en apellido materno", example = "García")
    private String apellidoMaterno;

    @Schema(description = "Filtrar por estatus activo (true/false)", example = "true")
    private Boolean activo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha inicial de creación (ISO 8601)", example = "2024-01-01T00:00:00")
    private LocalDateTime fechaInicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Fecha final de creación (ISO 8601)", example = "2026-12-31T23:59:59")
    private LocalDateTime fechaFin;

    @Min(value = 0, message = "La página debe ser mayor o igual a 0")
    @Builder.Default
    @Schema(description = "Número de página (iniciando en 0)", example = "0", defaultValue = "0")
    private Integer pagina = 0;

    @Min(value = 1, message = "El tamaño de página debe ser mínimo 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    @Builder.Default
    @Schema(description = "Cantidad de registros por página", example = "20", defaultValue = "20")
    private Integer tamanoPagina = 20;

    @Builder.Default
    @Schema(description = "Campo por el cual ordenar los resultados", example = "id", defaultValue = "id")
    private String ordenarPor = "id";

    @Builder.Default
    @Schema(description = "Dirección del ordenamiento (ASC o DESC)", example = "ASC", defaultValue = "ASC")
    private String direccionOrden = "ASC";
}
