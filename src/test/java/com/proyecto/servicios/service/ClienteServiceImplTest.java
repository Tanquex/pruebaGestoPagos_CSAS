package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.DomicilioDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.GeneradorCuentaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private GeneradorCuentaUtil generadorCuentaUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private RegistroClienteRequest requestValido;

    @BeforeEach
    void setUp() {
        DomicilioDto domicilioDto = DomicilioDto.builder()
                .calle("Av. Reforma")
                .numeroExterior("123")
                .colonia("Juárez")
                .municipio("Cuauhtémoc")
                .estado("CDMX")
                .codigoPostal("06600")
                .pais("México")
                .build();

        requestValido = RegistroClienteRequest.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 6, 15))
                .curp("PELJ950615HDFRMN01")
                .rfc("PELJ9506151A2")
                .sexo("M")
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correo("juan.perez@example.com")
                .telefonoMovil("5512345678")
                .ocupacion("Ingeniero")
                .empresa("Tech Co")
                .ingresoMensual(new BigDecimal("30000.00"))
                .saldoInicial(new BigDecimal("1000.00"))
                .password("PasswordSeguro123")
                .domicilio(domicilioDto)
                .build();
    }

    @Test
    @DisplayName("Registrar cliente exitoso crea Domicilio, Cliente, Cuenta y Usuario")
    void testRegistrarCliente_Exitoso() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);

        Domicilio domGuardado = requestValido.getDomicilio().toEntity();
        domGuardado.setId(1L);
        when(domicilioRepository.save(any(Domicilio.class))).thenReturn(domGuardado);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(10L);
            c.setCuentas(new ArrayList<>());
            return c;
        });

        when(generadorCuentaUtil.generarNumeroCuentaUnico()).thenReturn("4152123456789012");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta c = invocation.getArgument(0);
            c.setId(100L);
            return c;
        });

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHashPassword");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(50L);
            return u;
        });

        ClienteResponseDto resultado = clienteService.registrarCliente(requestValido);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Juan", resultado.getNombre());
        assertEquals("PELJ950615HDFRMN01", resultado.getCurp());
        assertEquals(1, resultado.getCuentas().size());
        assertEquals("4152123456789012", resultado.getCuentas().get(0).getNumeroCuenta());
        assertTrue(resultado.getActivo());

        verify(domicilioRepository).save(any(Domicilio.class));
        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Rechazar registro si el cliente es menor de edad (<18 años)")
    void testRegistrarCliente_MenorDeEdad_LanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertTrue(ex.getMessage().contains("mayor de edad"));
        verifyNoInteractions(domicilioRepository, cuentaRepository, usuarioRepository);
    }

    @Test
    @DisplayName("Rechazar registro si la CURP ya existe")
    void testRegistrarCliente_CurpDuplicada_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(requestValido.getCurp())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () ->
                clienteService.registrarCliente(requestValido));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar registro si el RFC ya existe")
    void testRegistrarCliente_RfcDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(requestValido.getRfc())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    @Test
    @DisplayName("Rechazar registro si el Correo ya existe")
    void testRegistrarCliente_CorreoDuplicado_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(requestValido.getCorreo())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () ->
                clienteService.registrarCliente(requestValido));
    }

    @Test
    @DisplayName("Buscar cliente por CURP exitoso")
    void testObtenerPorCurp_Exitoso() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .curp("PELJ950615HDFRMN01")
                .rfc("PELJ9506151A2")
                .correo("juan@test.com")
                .activo(true)
                .build();

        when(clienteRepository.findByCurp("PELJ950615HDFRMN01")).thenReturn(Optional.of(cliente));

        ClienteResponseDto response = clienteService.obtenerPorCurp("PELJ950615HDFRMN01");
        assertNotNull(response);
        assertEquals("PELJ950615HDFRMN01", response.getCurp());
    }

    @Test
    @DisplayName("Buscar cliente por CURP inexistente lanza ClienteNoEncontradoException")
    void testObtenerPorCurp_NoEncontrado() {
        when(clienteRepository.findByCurp("INEXISTENTE12345678")).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () ->
                clienteService.obtenerPorCurp("INEXISTENTE12345678"));
    }

    @Test
    @DisplayName("Actualización parcial no permite modificar CURP ni RFC")
    void testActualizarParcial_Exitoso() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .curp("PELJ950615HDFRMN01")
                .rfc("PELJ9506151A2")
                .correo("juan@test.com")
                .telefonoMovil("5512345678")
                .activo(true)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        ActualizaClienteRequest updateReq = ActualizaClienteRequest.builder()
                .nombre("Juan Modificado")
                .telefonoMovil("5599887766")
                .build();

        ClienteResponseDto actualizado = clienteService.actualizarParcial(1L, updateReq);
        assertEquals("Juan Modificado", actualizado.getNombre());
        assertEquals("5599887766", actualizado.getTelefonoMovil());
        // Se preservan los datos oficiales
        assertEquals("PELJ950615HDFRMN01", actualizado.getCurp());
        assertEquals("PELJ9506151A2", actualizado.getRfc());
    }

    @Test
    @DisplayName("Baja lógica en cascada desactiva cliente, cuentas y usuario")
    void testDarDeBajaLogica_Cascada() {
        Cuenta cuenta = Cuenta.builder()
                .id(100L)
                .numeroCuenta("4152123456789012")
                .estatus("ACTIVA")
                .activo(true)
                .build();

        Usuario usuario = Usuario.builder()
                .id(50L)
                .correo("juan@test.com")
                .activo(true)
                .build();

        Cliente cliente = Cliente.builder()
                .id(1L)
                .activo(true)
                .cuentas(new ArrayList<>(java.util.List.of(cuenta)))
                .usuario(usuario)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.darDeBajaLogica(1L);

        assertFalse(cliente.getActivo());
        assertFalse(cuenta.getActivo());
        assertEquals("INACTIVA", cuenta.getEstatus());
        assertFalse(usuario.getActivo());

        verify(clienteRepository).save(cliente);
    }
}
