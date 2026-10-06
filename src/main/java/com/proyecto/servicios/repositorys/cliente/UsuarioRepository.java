package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByClienteId(Long clienteId);

    boolean existsByCorreo(String correo);

    boolean existsByClienteId(Long clienteId);

    org.springframework.data.domain.Page<Usuario> findByActivo(Boolean activo, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Usuario> findByCorreoContainingIgnoreCase(String correo, org.springframework.data.domain.Pageable pageable);
}
