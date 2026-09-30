package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreo(String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    Page<Cliente> findByActivoTrue(Pageable pageable);

    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Cliente> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno, Pageable pageable);

    Page<Cliente> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno, Pageable pageable);

    Page<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cu WHERE cu.numeroCuenta = :numeroCuenta")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);
}
