package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;

import java.util.List;
import java.util.Optional;

public interface CatalogoProductoService {

    List<ProductoDocument> sincronizarCatalogo();

    List<ProductoDocument> obtenerTodos();

    List<ProductoDocument> obtenerPorServicio(Integer idServicio);

    List<ProductoDocument> buscarPorNombre(String nombre);

    Optional<ProductoDocument> obtenerPorIdProducto(Integer idProducto);
}
