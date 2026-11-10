package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.catalogo.ProductoConsultaRequest;
import com.proyecto.servicios.model.cliente.*;
import com.proyecto.servicios.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllersUnitTest {

    @Mock
    private ClienteService clienteService;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private PagoServicioService pagoServicioService;
    @Mock
    private AuthService authService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private CatalogoProductoService catalogoProductoService;
    @Mock
    private PersonaService personaService;

    @Test
    @DisplayName("ClienteController endpoints")
    void testClienteController() {
        ClienteController controller = new ClienteController(clienteService);

        ClienteResponseDto dto = new ClienteResponseDto();
        when(clienteService.registrarCliente(any())).thenReturn(dto);
        when(clienteService.consultarClientes(any())).thenReturn(new PageImpl<>(Collections.singletonList(dto)));
        when(clienteService.actualizarParcial(any())).thenReturn(dto);
        doNothing().when(clienteService).darDeBajaLogica(any());

        assertEquals(HttpStatus.CREATED, controller.registrarCliente(new RegistroClienteRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.consultarClientes(new ClienteConsultaRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.actualizarParcial(new ActualizaClienteRequest()).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.darDeBajaLogica(new BajaClienteRequest()).getStatusCode());
    }

    @Test
    @DisplayName("CuentaController endpoints")
    void testCuentaController() {
        CuentaController controller = new CuentaController(cuentaService);

        CuentaResponseDto dto = new CuentaResponseDto();
        when(cuentaService.crearCuenta(any())).thenReturn(dto);
        when(cuentaService.consultarCuentas(any())).thenReturn(new PageImpl<>(Collections.singletonList(dto)));
        when(cuentaService.actualizarEstatus(any())).thenReturn(dto);

        assertEquals(HttpStatus.CREATED, controller.crearCuenta(new CrearCuentaRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.consultarCuentas(new CuentaConsultaRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.actualizarEstatus(new ActualizaCuentaRequest()).getStatusCode());
    }

    @Test
    @DisplayName("PagoServicioController endpoints")
    void testPagoServicioController() {
        PagoServicioController controller = new PagoServicioController(pagoServicioService);

        PagoServicioResponseDto dto = new PagoServicioResponseDto();
        when(pagoServicioService.procesarPagoServicio(any())).thenReturn(dto);
        when(pagoServicioService.consultarTransacciones(any())).thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        assertEquals(HttpStatus.OK, controller.procesarPago(new PagoServicioRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.consultarTransacciones(new TransaccionConsultaRequest()).getStatusCode());
    }

    @Test
    @DisplayName("AuthController endpoints")
    void testAuthController() {
        AuthController controller = new AuthController(authService);

        LoginResponse resp = new LoginResponse();
        when(authService.login(any())).thenReturn(resp);

        assertEquals(HttpStatus.OK, controller.login(new LoginRequest()).getStatusCode());
    }

    @Test
    @DisplayName("UsuarioController endpoints")
    void testUsuarioController() {
        UsuarioController controller = new UsuarioController(usuarioService);

        UsuarioResponseDto dto = new UsuarioResponseDto();
        when(usuarioService.crearUsuario(any())).thenReturn(dto);
        when(usuarioService.consultarUsuarios(any())).thenReturn(new PageImpl<>(Collections.singletonList(dto)));

        assertEquals(HttpStatus.CREATED, controller.crearUsuario(new CrearUsuarioRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.consultarUsuarios(new UsuarioConsultaRequest()).getStatusCode());
    }

    @Test
    @DisplayName("CatalogoController endpoints")
    void testCatalogoController() {
        CatalogoController controller = new CatalogoController(catalogoProductoService);

        when(catalogoProductoService.consultarProductos(any())).thenReturn(Collections.emptyList());
        when(catalogoProductoService.sincronizarCatalogo()).thenReturn(Collections.emptyList());

        assertEquals(HttpStatus.OK, controller.consultarProductos(new ProductoConsultaRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.sincronizarManual().getStatusCode());
    }

    @Test
    @DisplayName("PersonaController endpoints")
    void testPersonaController() {
        PersonaController controller = new PersonaController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "personaService", personaService);

        when(personaService.creaPersona(any())).thenReturn(new PersonaResponse());
        when(personaService.actualizaPersona(any())).thenReturn(new GenericResponse());
        when(personaService.eliminaPersona(any())).thenReturn(new GenericResponse());

        assertEquals(HttpStatus.OK, controller.crearUser(new PersonasRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.eliminaUser(new EliminaPersonaRequest()).getStatusCode());
        assertEquals(HttpStatus.OK, controller.actualizUser(new PersonasRequest()).getStatusCode());
    }

    @Test
    @DisplayName("HomeController endpoints")
    void testHomeController() {
        HomeController controller = new HomeController();
        assertEquals("redirect:/swagger-ui/index.html", controller.index());
    }
}
