package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Domicilio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomicilioRepository extends JpaRepository<Domicilio, Long> {
}
