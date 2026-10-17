package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.catalogo.ProductoConsultaRequest;
import com.proyecto.servicios.model.catalogo.ProductoResponseDto;
import com.proyecto.servicios.service.CatalogoProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo de Productos", description = "Endpoints para consulta dinámica y sincronización del catálogo GestoPago en MongoDB")
@Slf4j
public class CatalogoController {

    private final CatalogoProductoService catalogoService;

    public CatalogoController(CatalogoProductoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @PostMapping(value = "/productos/consultar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar catálogo de productos (Filtros combinados)",
            description = "Único endpoint para consultar productos en MongoDB por ID de producto, ID de servicio o texto libre en el cuerpo JSON.")
    public ResponseEntity<List<ProductoResponseDto>> consultarProductos(@RequestBody(required = false) ProductoConsultaRequest request) {
        log.info("Recibida petición POST /api/v1/catalogo/productos/consultar");
        List<ProductoDocument> docs = catalogoService.consultarProductos(request);
        List<ProductoResponseDto> dtos = docs.stream()
                .map(ProductoResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sincronización manual del catálogo",
            description = "Invoca a GestoPago, procesa el XML y actualiza MongoDB bajo demanda.")
    public ResponseEntity<GenericResponse> sincronizarManual() {
        log.info("Recibida petición POST /api/v1/catalogo/sincronizar");
        List<ProductoDocument> sincronizados = catalogoService.sincronizarCatalogo();
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Catálogo sincronizado exitosamente. Total de productos: " + sincronizados.size());
        return ResponseEntity.ok(response);
    }
}
