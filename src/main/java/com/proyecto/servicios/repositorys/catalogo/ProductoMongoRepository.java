package com.proyecto.servicios.repositorys.catalogo;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoMongoRepository extends MongoRepository<ProductoDocument, String> {

    Optional<ProductoDocument> findByIdProducto(Integer idProducto);

    List<ProductoDocument> findByIdServicio(Integer idServicio);

    List<ProductoDocument> findByTipoFront(Integer tipoFront);

    List<ProductoDocument> findByServicioContainingIgnoreCase(String servicio);
}
