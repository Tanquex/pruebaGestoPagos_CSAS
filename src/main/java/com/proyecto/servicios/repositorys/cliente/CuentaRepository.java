package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cuenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long>, JpaSpecificationExecutor<Cuenta> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteId(Long clienteId);

    Page<Cuenta> findByClienteId(Long clienteId, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaConBloqueo(@org.springframework.data.repository.query.Param("numeroCuenta") String numeroCuenta);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Cuenta c SET c.saldo = c.saldo - :monto WHERE c.numeroCuenta = :numeroCuenta AND c.saldo >= :monto")
    int descontarSaldoSiSuficiente(@org.springframework.data.repository.query.Param("numeroCuenta") String numeroCuenta,
                                  @org.springframework.data.repository.query.Param("monto") java.math.BigDecimal monto);
}

