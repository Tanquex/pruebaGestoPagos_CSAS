package com.proyecto.servicios.entity.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "catalogo_productos")
public class ProductoDocument {

    @Id
    private String id;

    @Indexed
    private Integer idProducto;

    @Indexed
    private Integer idServicio;

    private String servicio;

    private String producto;

    private Integer idCatTipoServicio;

    private Integer tipoFront;

    private Boolean hasDigitoVerificador;

    private Double precio;

    private Boolean showAyuda;

    private String tipoReferencia;

    private String legend;

    private LocalDateTime fechaSincronizacion;
}
