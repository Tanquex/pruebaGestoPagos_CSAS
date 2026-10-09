package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de actualización parcial de cliente (No permite modificar CURP ni RFC)")
public class ActualizaClienteRequest {

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Schema(example = "Juan")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no debe exceder 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El segundo nombre solo debe contener letras y espacios")
    @Schema(example = "Carlos")
    private String segundoNombre;

    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Schema(example = "Pérez")
    private String apellidoPaterno;

    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^$|^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Schema(example = "Gómez")
    private String apellidoMaterno;

    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Schema(example = "juan.actualizado@example.com")
    private String correo;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    @Schema(example = "5598765432")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    @Schema(example = "5511223344")
    private String telefonoAlternativo;

    @Schema(example = "CASADO")
    private String estadoCivil;

    @Size(max = 100, message = "La ocupación no debe exceder 100 caracteres")
    @Schema(example = "Líder Técnico")
    private String ocupacion;

    @Size(max = 100, message = "La empresa no debe exceder 100 caracteres")
    @Schema(example = "Innovaciones S.A.")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Schema(example = "45000.00")
    private BigDecimal ingresoMensual;

    @Valid
    private DomicilioDto domicilio;
}
