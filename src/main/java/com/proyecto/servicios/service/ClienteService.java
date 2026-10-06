package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ClienteService {

    ClienteResponseDto registrarCliente(RegistroClienteRequest request);

    Page<ClienteResponseDto> obtenerTodos(Pageable pageable);

    ClienteResponseDto obtenerPorId(Long id);

    ClienteResponseDto obtenerPorCurp(String curp);

    ClienteResponseDto obtenerPorRfc(String rfc);

    ClienteResponseDto obtenerPorCorreo(String correo);

    ClienteResponseDto obtenerPorNumeroCuenta(String numeroCuenta);

    Page<ClienteResponseDto> buscarPorNombre(String nombre, Pageable pageable);

    Page<ClienteResponseDto> buscarPorApellidoPaterno(String apellidoPaterno, Pageable pageable);

    Page<ClienteResponseDto> buscarPorApellidoMaterno(String apellidoMaterno, Pageable pageable);

    Page<ClienteResponseDto> obtenerActivos(Pageable pageable);

    Page<ClienteResponseDto> obtenerPorRangoFechas(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    ClienteResponseDto actualizarParcial(Long id, ActualizaClienteRequest request);

    void darDeBajaLogica(Long id);
}
