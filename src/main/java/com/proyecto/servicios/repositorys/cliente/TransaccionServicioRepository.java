package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.TransaccionServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionServicioRepository extends JpaRepository<TransaccionServicio, Long>, JpaSpecificationExecutor<TransaccionServicio> {

    Optional<TransaccionServicio> findByRandomString(String randomString);

    boolean existsByRandomString(String randomString);

    List<TransaccionServicio> findByCuentaId(Long cuentaId);
}
