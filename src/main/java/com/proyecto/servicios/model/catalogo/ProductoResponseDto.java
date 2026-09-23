package com.proyecto.servicios.model.catalogo;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponseDto {

    private Integer idProducto;
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

    public static ProductoResponseDto fromEntity(ProductoDocument doc) {
        if (doc == null) return null;
        return ProductoResponseDto.builder()
                .idProducto(doc.getIdProducto())
                .idServicio(doc.getIdServicio())
                .servicio(doc.getServicio())
                .producto(doc.getProducto())
                .idCatTipoServicio(doc.getIdCatTipoServicio())
                .tipoFront(doc.getTipoFront())
                .hasDigitoVerificador(doc.getHasDigitoVerificador())
                .precio(doc.getPrecio())
                .showAyuda(doc.getShowAyuda())
                .tipoReferencia(doc.getTipoReferencia())
                .legend(doc.getLegend())
                .fechaSincronizacion(doc.getFechaSincronizacion())
                .build();
    }
}
