package com.proyecto.servicios.model.cliente;

import com.proyecto.servicios.entity.cliente.Domicilio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de domicilio del cliente")
public class DomicilioDto {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 2, max = 150, message = "La calle debe tener entre 2 y 150 caracteres")
    @Schema(example = "Av. Reforma")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no debe exceder 20 caracteres")
    @Schema(example = "222")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no debe exceder 20 caracteres")
    @Schema(example = "Piso 5, Depto 501")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 100, message = "La colonia debe tener entre 2 y 100 caracteres")
    @Schema(example = "Juárez")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio")
    @Size(min = 2, max = 100, message = "El municipio debe tener entre 2 y 100 caracteres")
    @Schema(example = "Cuauhtémoc")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(min = 2, max = 100, message = "El estado debe tener entre 2 y 100 caracteres")
    @Schema(example = "Ciudad de México")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    @Schema(example = "06600")
    private String codigoPostal;

    @Builder.Default
    @Schema(example = "México")
    private String pais = "México";

    public static DomicilioDto fromEntity(Domicilio entity) {
        if (entity == null) return null;
        return DomicilioDto.builder()
                .calle(entity.getCalle())
                .numeroExterior(entity.getNumeroExterior())
                .numeroInterior(entity.getNumeroInterior())
                .colonia(entity.getColonia())
                .municipio(entity.getMunicipio())
                .estado(entity.getEstado())
                .codigoPostal(entity.getCodigoPostal())
                .pais(entity.getPais())
                .build();
    }

    public Domicilio toEntity() {
        return Domicilio.builder()
                .calle(this.calle)
                .numeroExterior(this.numeroExterior)
                .numeroInterior(this.numeroInterior)
                .colonia(this.colonia)
                .municipio(this.municipio)
                .estado(this.estado)
                .codigoPostal(this.codigoPostal)
                .pais(this.pais != null && !this.pais.isBlank() ? this.pais : "México")
                .build();
    }
}
