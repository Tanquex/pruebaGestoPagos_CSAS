package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Debe manejar MethodArgumentNotValidException con mapa de errores")
    void testHandleValidationExceptions() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "correo", "El correo es obligatorio"));
        bindingResult.addError(new FieldError("target", "curp", "CURP inválida"));

        java.lang.reflect.Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponseDto> response = handler.handleValidationExceptions(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getErrores().size());
        assertEquals("El correo es obligatorio", response.getBody().getErrores().get("correo"));
    }

    @Test
    @DisplayName("Debe manejar HttpMessageNotReadableException")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON malformado");
        ResponseEntity<ErrorResponseDto> response = handler.handleHttpMessageNotReadable(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("El formato del cuerpo JSON de la solicitud es inválido", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar ClienteYaExisteException y TransaccionDuplicadaException")
    void testHandleConflictExceptions() {
        ResponseEntity<ErrorResponseDto> resp1 = handler.handleClienteYaExiste(new ClienteYaExisteException("Cliente duplicado"));
        assertEquals(HttpStatus.CONFLICT, resp1.getStatusCode());
        assertEquals("Cliente duplicado", resp1.getBody().getMensaje());

        ResponseEntity<ErrorResponseDto> resp2 = handler.handleClienteYaExiste(new TransaccionDuplicadaException("Transacción duplicada"));
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());
        assertEquals("Transacción duplicada", resp2.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar DataIntegrityViolationException con causas de CURP, RFC, Correo, Cuenta y genérico")
    void testHandleDataIntegrityViolation() {
        // CURP
        DataIntegrityViolationException exCurp = new DataIntegrityViolationException("error", new RuntimeException("uq_clientes_curp constraint violation"));
        ResponseEntity<ErrorResponseDto> r1 = handler.handleDataIntegrityViolation(exCurp);
        assertEquals(HttpStatus.CONFLICT, r1.getStatusCode());
        assertEquals("Ya existe un cliente registrado con la misma CURP.", r1.getBody().getMensaje());

        // RFC
        DataIntegrityViolationException exRfc = new DataIntegrityViolationException("error", new RuntimeException("uq_clientes_rfc constraint violation"));
        ResponseEntity<ErrorResponseDto> r2 = handler.handleDataIntegrityViolation(exRfc);
        assertEquals("Ya existe un cliente registrado con el mismo RFC.", r2.getBody().getMensaje());

        // Correo
        DataIntegrityViolationException exCorreo = new DataIntegrityViolationException("error", new RuntimeException("uq_clientes_correo constraint violation"));
        ResponseEntity<ErrorResponseDto> r3 = handler.handleDataIntegrityViolation(exCorreo);
        assertEquals("Ya existe un cliente registrado con el mismo correo electrónico.", r3.getBody().getMensaje());

        // Cuenta
        DataIntegrityViolationException exCuenta = new DataIntegrityViolationException("error", new RuntimeException("uq_cuentas_numero constraint violation"));
        ResponseEntity<ErrorResponseDto> r4 = handler.handleDataIntegrityViolation(exCuenta);
        assertEquals("El número de cuenta ya se encuentra registrado.", r4.getBody().getMensaje());

        // Genérico
        DataIntegrityViolationException exGen = new DataIntegrityViolationException("error", new RuntimeException("otro_constraint"));
        ResponseEntity<ErrorResponseDto> r5 = handler.handleDataIntegrityViolation(exGen);
        assertTrue(r5.getBody().getMensaje().contains("Conflicto de integridad"));
    }

    @Test
    @DisplayName("Debe manejar excepciones de tipo NoEncontrado (Cliente, Cuenta, Usuario)")
    void testHandleNoEncontrado() {
        ResponseEntity<ErrorResponseDto> r1 = handler.handleNoEncontrado(new ClienteNoEncontradoException("Cliente no existe"));
        assertEquals(HttpStatus.NOT_FOUND, r1.getStatusCode());

        ResponseEntity<ErrorResponseDto> r2 = handler.handleNoEncontrado(new CuentaNoEncontradaException("Cuenta no existe"));
        assertEquals(HttpStatus.NOT_FOUND, r2.getStatusCode());

        ResponseEntity<ErrorResponseDto> r3 = handler.handleNoEncontrado(new UsuarioNoEncontradoException("Usuario no existe"));
        assertEquals(HttpStatus.NOT_FOUND, r3.getStatusCode());
    }

    @Test
    @DisplayName("Debe manejar CredencialesInvalidasException con 401")
    void testHandleCredencialesInvalidas() {
        ResponseEntity<ErrorResponseDto> r = handler.handleCredencialesInvalidas(new CredencialesInvalidasException("Password erróneo"));
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals("Password erróneo", r.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar ValidacionNegocioException con 422")
    void testHandleValidacionNegocio() {
        ResponseEntity<ErrorResponseDto> r = handler.handleValidacionNegocio(new ValidacionNegocioException("Menor de edad"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, r.getStatusCode());
        assertEquals("Menor de edad", r.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar GestoPagoResultadoInciertoException con 504 Gateway Timeout")
    void testHandleResultadoIncierto() {
        ResponseEntity<ErrorResponseDto> r = handler.handleResultadoIncierto(new GestoPagoResultadoInciertoException("Timeout en GestoPago"));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, r.getStatusCode());
        assertEquals("Timeout en GestoPago", r.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar GestoPagoIntegrationException con 502 Bad Gateway")
    void testHandleIntegrationException() {
        ResponseEntity<ErrorResponseDto> r = handler.handleIntegrationException(new GestoPagoIntegrationException("Error de conexión 500"));
        assertEquals(HttpStatus.BAD_GATEWAY, r.getStatusCode());
        assertEquals("Error de conexión 500", r.getBody().getMensaje());
    }

    @Test
    @DisplayName("Debe manejar NoResourceFoundException con 404")
    void testHandleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/ruta/inexistente");
        ResponseEntity<ErrorResponseDto> r = handler.handleNoResourceFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertTrue(r.getBody().getMensaje().contains("/ruta/inexistente"));
    }

    @Test
    @DisplayName("Debe manejar Exception genérica con 500")
    void testHandleGenericException() {
        ResponseEntity<ErrorResponseDto> r = handler.handleGenericException(new RuntimeException("Error inesperado"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, r.getStatusCode());
        assertEquals("Ocurrió un error interno al procesar la solicitud.", r.getBody().getMensaje());
    }
}

