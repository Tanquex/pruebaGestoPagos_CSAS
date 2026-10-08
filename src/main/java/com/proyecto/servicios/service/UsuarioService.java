package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.CrearUsuarioRequest;
import com.proyecto.servicios.model.cliente.UsuarioResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponseDto crearUsuario(CrearUsuarioRequest request);

    UsuarioResponseDto obtenerPorId(Long id);

    UsuarioResponseDto obtenerPorClienteId(Long clienteId);

    UsuarioResponseDto obtenerPorCorreo(String correo);

    Page<UsuarioResponseDto> obtenerTodos(Pageable pageable);

    Page<UsuarioResponseDto> obtenerPorActivo(Boolean activo, Pageable pageable);

    Page<UsuarioResponseDto> buscarPorCorreo(String correo, Pageable pageable);
}
