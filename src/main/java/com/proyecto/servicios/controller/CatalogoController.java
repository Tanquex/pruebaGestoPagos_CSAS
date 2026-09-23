package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.catalogo.ProductoResponseDto;
import com.proyecto.servicios.service.CatalogoProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo de Productos", description = "Endpoints para consulta y sincronización del catálogo GestoPago almacenado en MongoDB")
@Slf4j
public class CatalogoController {

    private final CatalogoProductoService catalogoService;

    public CatalogoController(CatalogoProductoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar catálogo de productos", description = "Devuelve los productos almacenados en MongoDB en formato JSON, con filtros opcionales")
    public ResponseEntity<List<ProductoResponseDto>> listarProductos(
            @RequestParam(required = false) Integer idServicio,
            @RequestParam(required = false) String buscar) {

        List<ProductoDocument> docs;
        if (idServicio != null) {
            docs = catalogoService.obtenerPorServicio(idServicio);
        } else if (buscar != null && !buscar.isBlank()) {
            docs = catalogoService.buscarPorNombre(buscar);
        } else {
            docs = catalogoService.obtenerTodos();
        }

        List<ProductoResponseDto> dtos = docs.stream()
                .map(ProductoResponseDto::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping(value = "/productos/{idProducto}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtener producto por ID", description = "Devuelve los detalles de un producto específico")
    public ResponseEntity<ProductoResponseDto> obtenerPorId(@PathVariable Integer idProducto) {
        return catalogoService.obtenerPorIdProducto(idProducto)
                .map(doc -> ResponseEntity.ok(ProductoResponseDto.fromEntity(doc)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping(value = "/sincronizar", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sincronización manual del catálogo", description = "Invoca a GestoPago, procesa el XML y actualiza MongoDB bajo demanda")
    public ResponseEntity<GenericResponse> sincronizarManual() {
        List<ProductoDocument> sincronizados = catalogoService.sincronizarCatalogo();
        GenericResponse response = new GenericResponse();
        response.setCodigo(0);
        response.setMensaje("Catálogo sincronizado exitosamente. Total de productos: " + sincronizados.size());
        return ResponseEntity.ok(response);
    }
}
