package com.proyecto.servicios.model.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud completa de Onboarding para Cliente Persona Física")
public class RegistroClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Schema(example = "Juan")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no debe exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    @Schema(example = "Carlos")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Schema(example = "Pérez")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    @Schema(example = "Gómez")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @Schema(example = "1990-05-15")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "La CURP debe tener formato oficial mexicano de 18 caracteres (RENAPO)")
    @Schema(example = "PEGJ900515HDFRMN02")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}\\d{6}[A-Z0-9]{3}$", message = "El RFC debe tener formato oficial mexicano de 12 o 13 caracteres (SAT)")
    @Schema(example = "PEGJ9005151A2")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^(?i)(MASCULINO|FEMENINO|H|M)$", message = "El sexo debe ser MASCULINO o FEMENINO (o H/M)")
    @Schema(example = "MASCULINO")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Schema(example = "Mexicana")
    @Builder.Default
    private String nacionalidad = "Mexicana";

    @NotBlank(message = "El estado civil es obligatorio")
    @Schema(example = "SOLTERO")
    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Schema(example = "juan.perez@example.com")
    private String correo;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    @Schema(example = "5512345678")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^\\d{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos si se proporciona")
    @Schema(example = "5587654321")
    private String telefonoAlternativo;

    @NotNull(message = "Los datos del domicilio son obligatorios")
    @Valid
    private DomicilioDto domicilio;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(max = 100, message = "La ocupación no debe exceder 100 caracteres")
    @Schema(example = "Ingeniero de Software")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(max = 100, message = "El nombre de la empresa no debe exceder 100 caracteres")
    @Schema(example = "Tech Solutions S.A.")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Schema(example = "35000.00")
    private BigDecimal ingresoMensual;

    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Schema(example = "1000.00", description = "Saldo con el que se abrirá la cuenta bancaria automática (opcional, default 0.00)")
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @NotBlank(message = "La contraseña de usuario es obligatoria")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#\\-])[A-Za-z\\d@$!%*?&._#\\-]{8,}$",
        message = "La contraseña debe contener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial (@$!%*?&._#-)"
    )
    @Schema(example = "P@ssword123", description = "Contraseña para el usuario de acceso asociado (será cifrada con BCrypt)")
    private String password;
}
