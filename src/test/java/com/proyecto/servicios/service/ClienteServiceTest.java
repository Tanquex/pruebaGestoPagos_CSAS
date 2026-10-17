package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaExisteException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.ActualizaClienteRequest;
import com.proyecto.servicios.model.cliente.BajaClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteResponseDto;
import com.proyecto.servicios.model.cliente.DomicilioDto;
import com.proyecto.servicios.model.cliente.RegistroClienteRequest;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.util.GeneradorCuentaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

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
    private ClienteService clienteService;

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

        Domicilio domicilio = requestValido.getDomicilio().toEntity();
        domicilio.setId(10L);
        when(domicilioRepository.save(any(Domicilio.class))).thenReturn(domicilio);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        when(generadorCuentaUtil.generarNumeroCuentaUnico()).thenReturn("4152849102938475");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(invocation -> {
            Cuenta cu = invocation.getArgument(0);
            cu.setId(100L);
            return cu;
        });

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHashString12345");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(200L);
            return u;
        });

        ClienteResponseDto response = clienteService.registrarCliente(requestValido);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PELJ950615HDFRMN01", response.getCurp());
        assertEquals("PELJ9506151A2", response.getRfc());
        assertTrue(response.getActivo());
        assertNotNull(response.getDomicilio());
        assertFalse(response.getCuentas().isEmpty());
        assertEquals("4152849102938475", response.getCuentas().get(0).getNumeroCuenta());

        verify(domicilioRepository).save(any(Domicilio.class));
        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Rechazar registro si el cliente es menor de edad (< 18 años)")
    void testRegistrarCliente_MenorDeEdad_LanzaExcepcion() {
        requestValido.setFechaNacimiento(LocalDate.now().minusYears(16));

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class, () ->
                clienteService.registrarCliente(requestValido));

        assertTrue(ex.getMessage().contains("El cliente debe ser mayor de edad"));
        verifyNoInteractions(domicilioRepository, cuentaRepository, usuarioRepository);
    }

    @Test
    @DisplayName("Rechazar registro si la CURP ya existe")
    void testRegistrarCliente_CurpDuplicada_LanzaExcepcion() {
        when(clienteRepository.existsByCurp(requestValido.getCurp())).thenReturn(true);

        assertThrows(ClienteYaExisteException.class, () ->
                clienteService.registrarCliente(requestValido));

        verify(clienteRepository, never()).save(any(Cliente.class));
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
    @DisplayName("Consultar clientes dinámicamente con filtros combinados")
    void testConsultarClientes_Dinamico() {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Juan")
                .apellidoPaterno("Pérez")
                .apellidoMaterno("López")
                .curp("PELJ950615HDFRMN01")
                .rfc("PELJ9506151A2")
                .correo("juan@test.com")
                .activo(true)
                .cuentas(new ArrayList<>())
                .build();

        Page<Cliente> page = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ClienteConsultaRequest queryReq = ClienteConsultaRequest.builder()
                .nombre("Juan")
                .activo(true)
                .pagina(0)
                .tamanoPagina(10)
                .build();

        Page<ClienteResponseDto> result = clienteService.consultarClientes(queryReq);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PELJ950615HDFRMN01", result.getContent().get(0).getCurp());
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
                .id(1L)
                .nombre("Juan Modificado")
                .telefonoMovil("5599887766")
                .build();

        ClienteResponseDto actualizado = clienteService.actualizarParcial(updateReq);
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
                .cuentas(new ArrayList<>(List.of(cuenta)))
                .usuario(usuario)
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.darDeBajaLogica(new BajaClienteRequest(1L));

        assertFalse(cliente.getActivo());
        assertFalse(cuenta.getActivo());
        assertEquals("INACTIVA", cuenta.getEstatus());
        assertFalse(usuario.getActivo());

        verify(clienteRepository).save(cliente);
    }
}
