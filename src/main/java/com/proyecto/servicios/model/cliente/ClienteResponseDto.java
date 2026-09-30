package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta detallada con información completa del cliente y sus cuentas")
public class ClienteResponseDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Juan")
    private String nombre;

    @Schema(example = "Carlos")
    private String segundoNombre;

    @Schema(example = "Pérez")
    private String apellidoPaterno;

    @Schema(example = "Gómez")
    private String apellidoMaterno;

    @Schema(example = "Juan Carlos Pérez Gómez")
    private String nombreCompleto;

    @Schema(example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @Schema(example = "PEGJ900515HDFRMN02")
    private String curp;

    @Schema(example = "PEGJ9005151A2")
    private String rfc;

    @Schema(example = "MASCULINO")
    private String sexo;

    @Schema(example = "Mexicana")
    private String nacionalidad;

    @Schema(example = "SOLTERO")
    private String estadoCivil;

    @Schema(example = "juan.perez@example.com")
    private String correo;

    @Schema(example = "5512345678")
    private String telefonoMovil;

    @Schema(example = "5587654321")
    private String telefonoAlternativo;

    @Schema(example = "Ingeniero de Software")
    private String ocupacion;

    @Schema(example = "Tech Solutions S.A.")
    private String empresa;

    @Schema(example = "35000.00")
    private BigDecimal ingresoMensual;

    @Schema(example = "true")
    private Boolean activo;

    private DomicilioDto domicilio;

    @Builder.Default
    private List<CuentaResponseDto> cuentas = new ArrayList<>();

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static ClienteResponseDto fromEntity(Cliente entity) {
        if (entity == null) return null;

        StringBuilder sb = new StringBuilder(entity.getNombre());
        if (entity.getSegundoNombre() != null && !entity.getSegundoNombre().isBlank()) {
            sb.append(" ").append(entity.getSegundoNombre());
        }
        sb.append(" ").append(entity.getApellidoPaterno()).append(" ").append(entity.getApellidoMaterno());

        List<CuentaResponseDto> cuentasDto = entity.getCuentas() != null
                ? entity.getCuentas().stream().map(CuentaResponseDto::fromEntity).toList()
                : new ArrayList<>();

        return ClienteResponseDto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .segundoNombre(entity.getSegundoNombre())
                .apellidoPaterno(entity.getApellidoPaterno())
                .apellidoMaterno(entity.getApellidoMaterno())
                .nombreCompleto(sb.toString())
                .fechaNacimiento(entity.getFechaNacimiento())
                .curp(entity.getCurp())
                .rfc(entity.getRfc())
                .sexo(entity.getSexo())
                .nacionalidad(entity.getNacionalidad())
                .estadoCivil(entity.getEstadoCivil())
                .correo(entity.getCorreo())
                .telefonoMovil(entity.getTelefonoMovil())
                .telefonoAlternativo(entity.getTelefonoAlternativo())
                .ocupacion(entity.getOcupacion())
                .empresa(entity.getEmpresa())
                .ingresoMensual(entity.getIngresoMensual())
                .activo(entity.getActivo())
                .domicilio(DomicilioDto.fromEntity(entity.getDomicilio()))
                .cuentas(cuentasDto)
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}
