package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.catalogo.ProductoDocument;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.TransaccionServicio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.PagoServicioRequest;
import com.proyecto.servicios.model.cliente.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.TransaccionServicioRepository;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionServicioRepository transaccionRepository;

    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private GestoPagoTxClient txClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AesEncryptionUtil aesUtil;
    private JaxbXmlParser jaxbXmlParser;
    private ObjectMapper objectMapper;

    private PagoServicioService pagoServicioService;

    private Cuenta cuentaMock;
    private ProductoDocument productoMock;
    private GestoPagoToken tokenMock;

    @BeforeEach
    void setUp() {
        aesUtil = new AesEncryptionUtil();
        jaxbXmlParser = new JaxbXmlParser();
        objectMapper = new ObjectMapper();

        pagoServicioService = new PagoServicioService(
                cuentaRepository,
                transaccionRepository,
                productoMongoRepository,
                tokenService,
                txClient,
                aesUtil,
                passwordEncoder,
                jaxbXmlParser,
                objectMapper
        );

        ReflectionTestUtils.setField(pagoServicioService, "defaultDistribuidor", 83);
        ReflectionTestUtils.setField(pagoServicioService, "defaultCodigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(pagoServicioService, "aesSecretKey", "GP2026_AES_KEY_!");

        Usuario usuarioMock = Usuario.builder()
                .id(1L)
                .correo("carlos@example.com")
                .password("$2a$10$encodedHash123")
                .activo(true)
                .build();

        Cliente clienteMock = Cliente.builder()
                .id(1L)
                .nombre("Carlos")
                .activo(true)
                .usuario(usuarioMock)
                .build();

        cuentaMock = Cuenta.builder()
                .id(10L)
                .numeroCuenta("4152849102938475")
                .saldo(new BigDecimal("2500.00"))
                .estatus("ACTIVA")
                .activo(true)
                .cliente(clienteMock)
                .build();

        productoMock = ProductoDocument.builder()
                .id("60a1b2c3d4e5f6")
                .idProducto(200)
                .idServicio(71)
                .servicio("Amazon")
                .producto("Amazon $100")
                .precio(100.0)
                .build();

        tokenMock = new GestoPagoToken();
        tokenMock.setToken("fake-jwt-token");
        tokenMock.setActivo(true);
    }

    @Test
    @DisplayName("Debe procesar el pago con éxito y descontar saldo ÚNICAMENTE cuando GestoPago responda código 01")
    void testProcesarPago_ExitosoCodigo01_DescuentaSaldo() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta("4152849102938475")
                .idServicio(71)
                .idProducto(200)
                .precio(new BigDecimal("100.00"))
                .telefonoReferencia("5512345678")
                .bandera("SIN_REFERENCIA")
                .pin("Password123*")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152849102938475")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.matches("Password123*", "$2a$10$encodedHash123")).thenReturn(true);
        when(productoMongoRepository.findByIdProductoAndIdServicio(200, 71)).thenReturn(Optional.of(productoMock));
        when(transaccionRepository.existsByRandomString(anyString())).thenReturn(false);
        when(transaccionRepository.save(any(TransaccionServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenService.obtenerTokenActivo(eq(83), anyString())).thenReturn(Optional.of(tokenMock));

        String xmlOk = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>01</CODIGO>\n" +
                "        <TEXTO>Operacion realizada con exito</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "    <TRANSACCION>\n" +
                "        <FOLIO>FOL-998877</FOLIO>\n" +
                "        <AUTORIZACION>AUT-445566</AUTORIZACION>\n" +
                "    </TRANSACCION>\n" +
                "</RESPONSE>";
        when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn(xmlOk);
        when(cuentaRepository.descontarSaldoSiSuficiente(eq("4152849102938475"), eq(new BigDecimal("100.00")))).thenReturn(1);

        PagoServicioResponseDto response = pagoServicioService.procesarPagoServicio(request);

        assertNotNull(response);
        assertEquals("EXITOSA", response.getEstatus());
        assertEquals("01", response.getCodigoProveedor());
        assertEquals("FOL-998877", response.getFolioProveedor());
        assertEquals("AUT-445566", response.getAutorizacionProveedor());
        assertEquals(new BigDecimal("2400.00"), response.getSaldoRestante());

        // Verificamos que se ejecutó el descuento en base de datos
        verify(cuentaRepository, times(1)).descontarSaldoSiSuficiente(eq("4152849102938475"), eq(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("NO debe descontar saldo si GestoPago responde con código de rechazo diferente de 01")
    void testProcesarPago_RechazoProveedor_NoDescuentaSaldo() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta("4152849102938475")
                .idServicio(71)
                .idProducto(200)
                .precio(new BigDecimal("100.00"))
                .telefonoReferencia("5512345678")
                .bandera("SIN_REFERENCIA")
                .pin("Password123*")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152849102938475")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.matches("Password123*", "$2a$10$encodedHash123")).thenReturn(true);
        when(productoMongoRepository.findByIdProductoAndIdServicio(200, 71)).thenReturn(Optional.of(productoMock));
        when(transaccionRepository.existsByRandomString(anyString())).thenReturn(false);
        when(transaccionRepository.save(any(TransaccionServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenService.obtenerTokenActivo(eq(83), anyString())).thenReturn(Optional.of(tokenMock));

        String xmlError = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>02</CODIGO>\n" +
                "        <TEXTO>Telefono invalido o sin conexion con carrier</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "</RESPONSE>";
        when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn(xmlError);

        PagoServicioResponseDto response = pagoServicioService.procesarPagoServicio(request);

        assertNotNull(response);
        assertEquals("RECHAZADA", response.getEstatus());
        assertEquals("02", response.getCodigoProveedor());
        assertEquals(new BigDecimal("2500.00"), response.getSaldoRestante());

        // Verificamos que NUNCA se descontó el saldo
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Debe rechazar la operación inmediatamente si el PIN de autorización es incorrecto")
    void testProcesarPago_PinIncorrecto_RechazaAutenticacion() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta("4152849102938475")
                .idServicio(71)
                .idProducto(200)
                .precio(new BigDecimal("100.00"))
                .telefonoReferencia("5512345678")
                .bandera("SIN_REFERENCIA")
                .pin("WrongPin")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152849102938475")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.matches("WrongPin", "$2a$10$encodedHash123")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> pagoServicioService.procesarPagoServicio(request));

        // No se llama a MongoDB ni a GestoPago
        verify(productoMongoRepository, never()).findByIdProductoAndIdServicio(anyInt(), anyInt());
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Debe rechazar si el saldo de la cuenta es menor al monto requerido")
    void testProcesarPago_SaldoInsuficiente_Rechaza() {
        cuentaMock.setSaldo(new BigDecimal("50.00"));

        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta("4152849102938475")
                .idServicio(71)
                .idProducto(200)
                .precio(new BigDecimal("100.00"))
                .telefonoReferencia("5512345678")
                .bandera("SIN_REFERENCIA")
                .pin("Password123*")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152849102938475")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.matches("Password123*", "$2a$10$encodedHash123")).thenReturn(true);

        assertThrows(ValidacionNegocioException.class, () -> pagoServicioService.procesarPagoServicio(request));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Debe marcar la transacción en EN_REVISION y NO descontar saldo ante timeout de red")
    void testProcesarPago_TimeoutGestoPago_EnRevisionSinDescuento() {
        PagoServicioRequest request = PagoServicioRequest.builder()
                .numCuenta("4152849102938475")
                .idServicio(71)
                .idProducto(200)
                .precio(new BigDecimal("100.00"))
                .telefonoReferencia("5512345678")
                .bandera("SIN_REFERENCIA")
                .pin("Password123*")
                .build();

        when(cuentaRepository.findByNumeroCuenta("4152849102938475")).thenReturn(Optional.of(cuentaMock));
        when(passwordEncoder.matches("Password123*", "$2a$10$encodedHash123")).thenReturn(true);
        when(productoMongoRepository.findByIdProductoAndIdServicio(200, 71)).thenReturn(Optional.of(productoMock));
        when(transaccionRepository.existsByRandomString(anyString())).thenReturn(false);
        when(transaccionRepository.save(any(TransaccionServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenService.obtenerTokenActivo(eq(83), anyString())).thenReturn(Optional.of(tokenMock));

        Request feignRequest = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                java.util.Collections.emptyMap(), null, java.nio.charset.StandardCharsets.UTF_8, null);
        when(txClient.enviarTransaccionJson(anyString(), anyString()))
                .thenThrow(new RetryableException(504, "Read timed out", Request.HttpMethod.POST, new Date(), feignRequest));

        assertThrows(GestoPagoIntegrationException.class, () -> pagoServicioService.procesarPagoServicio(request));

        // Se verifica que NUNCA se descontó el saldo
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }
}
