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
import com.proyecto.servicios.exception.GestoPagoResultadoInciertoException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.PagoServicioRequest;
import com.proyecto.servicios.model.cliente.PagoServicioResponseDto;
import com.proyecto.servicios.repositorys.catalogo.ProductoMongoRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.TransaccionServicioRepository;
import com.proyecto.servicios.util.AesEncryptionUtil;
import com.proyecto.servicios.util.JaxbXmlParser;
import feign.FeignException;
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
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PagoServicioServiceTest {

    private static final String NUM_CUENTA = "4152849102938475";
    private static final String HASH = "$2a$10$encodedHash123";
    private static final String XML_EXITO = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>"
            + "<TRANSACCION><FOLIO>FOL-998877</FOLIO><AUTORIZACION>AUT-445566</AUTORIZACION></TRANSACCION></RESPONSE>";
    private static final String XML_RECHAZO = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<RESPONSE><MENSAJE><CODIGO>02</CODIGO><TEXTO>Telefono invalido</TEXTO></MENSAJE></RESPONSE>";

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
    @Mock
    private PlatformTransactionManager transactionManager;

    private PagoServicioService servicio;
    private Cuenta cuenta;
    private final AtomicReference<TransaccionServicio> guardada = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        servicio = new PagoServicioService(cuentaRepository, transaccionRepository, productoMongoRepository,
                tokenService, txClient, new AesEncryptionUtil(), passwordEncoder, new JaxbXmlParser(),
                new ObjectMapper(), transactionManager);
        ReflectionTestUtils.setField(servicio, "defaultDistribuidor", 83);
        ReflectionTestUtils.setField(servicio, "defaultCodigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(servicio, "aesSecretKey", "clave-de-prueba-1");

        Usuario usuario = Usuario.builder().id(1L).correo("carlos@example.com").password(HASH).activo(true).build();
        Cliente cliente = Cliente.builder().id(1L).nombre("Carlos").activo(true).usuario(usuario).build();
        cuenta = Cuenta.builder().id(10L).numeroCuenta(NUM_CUENTA).saldo(new BigDecimal("2500.00"))
                .estatus("ACTIVA").activo(true).cliente(cliente).build();

        lenient().when(cuentaRepository.findByNumeroCuentaConBloqueo(NUM_CUENTA)).thenReturn(Optional.of(cuenta));
        lenient().when(passwordEncoder.matches("Password123*", HASH)).thenReturn(true);
        lenient().when(transaccionRepository.existsByRandomString(anyString())).thenReturn(false);
        lenient().when(transaccionRepository.sumarMontosComprometidos(10L)).thenReturn(BigDecimal.ZERO);
        lenient().when(productoMongoRepository.findByIdProductoAndIdServicio(200, 71)).thenReturn(Optional.of(
                ProductoDocument.builder().id("mongo-id").idProducto(200).idServicio(71)
                        .servicio("Amazon").producto("Amazon $100").precio(100.0).build()));
        lenient().when(transaccionRepository.save(any(TransaccionServicio.class))).thenAnswer(inv -> {
            TransaccionServicio tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(100L);
            }
            guardada.set(tx);
            return tx;
        });
        lenient().when(transaccionRepository.findById(100L)).thenAnswer(inv -> Optional.ofNullable(guardada.get()));

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("fake-jwt-token");
        lenient().when(tokenService.obtenerTokenActivo(eq(83), anyString())).thenReturn(Optional.of(token));
    }

    private PagoServicioRequest solicitud(String precio) {
        return PagoServicioRequest.builder().numCuenta(NUM_CUENTA).idServicio(71).idProducto(200)
                .precio(new BigDecimal(precio)).telefonoReferencia("5512345678").bandera("SIN_REFERENCIA")
                .pin("Password123*").randomString("TX-TEST-0001").build();
    }

    private RetryableException timeout() {
        Request feignRequest = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new RetryableException(504, "Read timed out", Request.HttpMethod.POST, new Date(), feignRequest);
    }

    @Test
    @DisplayName("Con código 01 descuenta el saldo y deja la transacción EXITOSA")
    void codigo01DescuentaSaldo() {
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn(XML_EXITO);
        org.mockito.Mockito.when(cuentaRepository.descontarSaldoSiSuficiente(NUM_CUENTA, new BigDecimal("100.00")))
                .thenAnswer(inv -> {
                    cuenta.setSaldo(new BigDecimal("2400.00"));
                    return 1;
                });

        PagoServicioResponseDto respuesta = servicio.procesarPagoServicio(solicitud("100.00"));

        assertEquals("EXITOSA", respuesta.getEstatus());
        assertEquals("01", respuesta.getCodigoProveedor());
        assertEquals("FOL-998877", respuesta.getFolioProveedor());
        assertEquals("AUT-445566", respuesta.getAutorizacionProveedor());
        assertEquals(new BigDecimal("2400.00"), respuesta.getSaldoRestante());
        assertEquals("4152********8475", respuesta.getNumCuenta());
        verify(cuentaRepository, times(1)).descontarSaldoSiSuficiente(NUM_CUENTA, new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Con un código distinto de 01 no se descuenta saldo y queda RECHAZADA")
    void rechazoNoDescuenta() {
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn(XML_RECHAZO);

        PagoServicioResponseDto respuesta = servicio.procesarPagoServicio(solicitud("100.00"));

        assertEquals("RECHAZADA", respuesta.getEstatus());
        assertEquals("02", respuesta.getCodigoProveedor());
        assertEquals(new BigDecimal("2500.00"), respuesta.getSaldoRestante());
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Un timeout deja la transacción EN_REVISION, conserva el registro y no descuenta saldo")
    void timeoutQuedaEnRevision() {
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString())).thenThrow(timeout());

        assertThrows(GestoPagoResultadoInciertoException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        assertNotNull(guardada.get());
        assertEquals("EN_REVISION", guardada.get().getEstatus());
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Un error 5xx sin cuerpo es incierto: EN_REVISION y sin descuento")
    void error5xxEsIncierto() {
        Request feignRequest = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString()))
                .thenThrow(new FeignException.InternalServerError("error", feignRequest, null, null));

        assertThrows(GestoPagoResultadoInciertoException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        assertEquals("EN_REVISION", guardada.get().getEstatus());
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Una respuesta ilegible con HTTP 200 no se toma como fallo: queda EN_REVISION")
    void respuestaIlegibleEsIncierta() {
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn("OK");

        assertThrows(GestoPagoResultadoInciertoException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        assertEquals("EN_REVISION", guardada.get().getEstatus());
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Si el proveedor confirma 01 pero ya no hay saldo, queda EN_REVISION para conciliar")
    void codigo01SinSaldoQuedaEnRevision() {
        org.mockito.Mockito.when(txClient.enviarTransaccionJson(anyString(), anyString())).thenReturn(XML_EXITO);
        org.mockito.Mockito.when(cuentaRepository.descontarSaldoSiSuficiente(NUM_CUENTA, new BigDecimal("100.00")))
                .thenReturn(0);

        PagoServicioResponseDto respuesta = servicio.procesarPagoServicio(solicitud("100.00"));

        assertEquals("EN_REVISION", respuesta.getEstatus());
        assertEquals("01", respuesta.getCodigoProveedor());
    }

    @Test
    @DisplayName("Sin token activo la operación no se envía y queda RECHAZADA sin cobro")
    void sinTokenNoSeEnvia() {
        org.mockito.Mockito.when(tokenService.obtenerTokenActivo(eq(83), anyString())).thenReturn(Optional.empty());

        assertThrows(GestoPagoIntegrationException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        assertEquals("RECHAZADA", guardada.get().getEstatus());
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
        verify(cuentaRepository, never()).descontarSaldoSiSuficiente(anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("Un PIN incorrecto se rechaza antes de tocar catálogo, base de datos o proveedor")
    void pinIncorrecto() {
        org.mockito.Mockito.when(passwordEncoder.matches("Password123*", HASH)).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        verify(productoMongoRepository, never()).findByIdProductoAndIdServicio(anyInt(), anyInt());
        verify(transaccionRepository, never()).save(any(TransaccionServicio.class));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Rechaza si el saldo no alcanza")
    void saldoInsuficiente() {
        cuenta.setSaldo(new BigDecimal("50.00"));

        assertThrows(ValidacionNegocioException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        verify(transaccionRepository, never()).save(any(TransaccionServicio.class));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("El saldo ya comprometido por operaciones EN_REVISION o PENDIENTES no se puede gastar dos veces")
    void saldoComprometidoNoSeGastaDosVeces() {
        org.mockito.Mockito.when(transaccionRepository.sumarMontosComprometidos(10L)).thenReturn(new BigDecimal("2450.00"));

        assertThrows(ValidacionNegocioException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        verify(transaccionRepository, never()).save(any(TransaccionServicio.class));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Un randomString repetido devuelve conflicto y no genera un segundo cargo")
    void randomStringDuplicado() {
        org.mockito.Mockito.when(transaccionRepository.existsByRandomString("TX-TEST-0001")).thenReturn(true);

        assertThrows(TransaccionDuplicadaException.class, () -> servicio.procesarPagoServicio(solicitud("100.00")));

        verify(transaccionRepository, never()).save(any(TransaccionServicio.class));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Un monto distinto al precio del catálogo se rechaza antes de llamar al proveedor")
    void montoNoCoincideConCatalogo() {
        assertThrows(ValidacionNegocioException.class, () -> servicio.procesarPagoServicio(solicitud("50.00")));

        verify(transaccionRepository, never()).save(any(TransaccionServicio.class));
        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }

    @Test
    @DisplayName("Tiempo aire exige un teléfono de 10 dígitos")
    void telefonoInvalidoParaTiempoAire() {
        PagoServicioRequest request = solicitud("100.00");
        request.setTelefonoReferencia("55123");

        assertThrows(ValidacionNegocioException.class, () -> servicio.procesarPagoServicio(request));

        verify(txClient, never()).enviarTransaccionJson(anyString(), anyString());
    }
}
