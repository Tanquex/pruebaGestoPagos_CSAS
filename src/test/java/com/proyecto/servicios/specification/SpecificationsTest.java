package com.proyecto.servicios.specification;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.TransaccionServicio;
import com.proyecto.servicios.entity.cliente.Usuario;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.CuentaConsultaRequest;
import com.proyecto.servicios.model.cliente.TransaccionConsultaRequest;
import com.proyecto.servicios.model.cliente.UsuarioConsultaRequest;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpecificationsTest {

    @Mock
    private Root<Cliente> clienteRoot;
    @Mock
    private Root<Cuenta> cuentaRoot;
    @Mock
    private Root<Usuario> usuarioRoot;
    @Mock
    private Root<TransaccionServicio> txRoot;

    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;
    @Mock
    private Expression<String> stringExpr;
    @Mock
    private Predicate predicate;
    @Mock
    private Join<Object, Object> join;

    @BeforeEach
    void setUp() {
        when(cb.conjunction()).thenReturn(predicate);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.upper(any())).thenReturn(stringExpr);
        when(cb.lower(any())).thenReturn(stringExpr);
        when(cb.between(any(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        when(clienteRoot.get(anyString())).thenReturn(path);
        when(clienteRoot.join(anyString(), any(JoinType.class))).thenReturn(join);
        when(cuentaRoot.get(anyString())).thenReturn(path);
        when(usuarioRoot.get(anyString())).thenReturn(path);
        when(txRoot.get(anyString())).thenReturn(path);
        when(txRoot.join(anyString(), any(JoinType.class))).thenReturn(join);
        when(path.get(anyString())).thenReturn(path);
        when(join.get(anyString())).thenReturn(path);
    }

    @Test
    @DisplayName("ClienteSpecification con filtro nulo y con todos los campos")
    void testClienteSpecification() {
        Specification<Cliente> specNulo = ClienteSpecification.conFiltros(null);
        assertNotNull(specNulo.toPredicate(clienteRoot, query, cb));

        ClienteConsultaRequest req = ClienteConsultaRequest.builder()
                .id(1L)
                .curp("CURP12345678901234")
                .rfc("RFC123456789")
                .correo("correo@test.com")
                .numeroCuenta("4152123456789012")
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .apellidoMaterno("Lopez")
                .activo(true)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now())
                .build();

        Specification<Cliente> spec = ClienteSpecification.conFiltros(req);
        assertNotNull(spec.toPredicate(clienteRoot, query, cb));

        // Solo fecha inicio
        req.setFechaFin(null);
        assertNotNull(ClienteSpecification.conFiltros(req).toPredicate(clienteRoot, query, cb));

        // Solo fecha fin
        req.setFechaInicio(null);
        req.setFechaFin(LocalDateTime.now());
        assertNotNull(ClienteSpecification.conFiltros(req).toPredicate(clienteRoot, query, cb));
    }

    @Test
    @DisplayName("CuentaSpecification con filtro nulo y con filtros completos")
    void testCuentaSpecification() {
        assertNotNull(CuentaSpecification.conFiltros(null).toPredicate(cuentaRoot, query, cb));

        CuentaConsultaRequest req = CuentaConsultaRequest.builder()
                .numeroCuenta("4152111122223333")
                .clienteId(5L)
                .estatus("ACTIVA")
                .activo(true)
                .build();

        assertNotNull(CuentaSpecification.conFiltros(req).toPredicate(cuentaRoot, query, cb));
    }

    @Test
    @DisplayName("UsuarioSpecification con filtro nulo y con filtros completos")
    void testUsuarioSpecification() {
        assertNotNull(UsuarioSpecification.conFiltros(null).toPredicate(usuarioRoot, query, cb));

        UsuarioConsultaRequest req = UsuarioConsultaRequest.builder()
                .id(1L)
                .clienteId(2L)
                .correo("user@test.com")
                .activo(true)
                .build();

        assertNotNull(UsuarioSpecification.conFiltros(req).toPredicate(usuarioRoot, query, cb));
    }

    @Test
    @DisplayName("TransaccionServicioSpecification con filtro nulo y con filtros completos")
    void testTransaccionServicioSpecification() {
        assertNotNull(TransaccionServicioSpecification.conFiltros(null).toPredicate(txRoot, query, cb));

        TransaccionConsultaRequest req = TransaccionConsultaRequest.builder()
                .id(10L)
                .randomString("RANDOM123")
                .numCuenta("4152999988887777")
                .idServicio(1)
                .idProducto(100)
                .estatus("EXITOSA")
                .fechaInicio(LocalDateTime.now().minusDays(2))
                .fechaFin(LocalDateTime.now())
                .build();

        assertNotNull(TransaccionServicioSpecification.conFiltros(req).toPredicate(txRoot, query, cb));

        // Fechas individuales
        req.setFechaFin(null);
        assertNotNull(TransaccionServicioSpecification.conFiltros(req).toPredicate(txRoot, query, cb));

        req.setFechaInicio(null);
        req.setFechaFin(LocalDateTime.now());
        assertNotNull(TransaccionServicioSpecification.conFiltros(req).toPredicate(txRoot, query, cb));
    }

    @Test
    @DisplayName("Constructores privados invocables por reflexión")
    void testConstructoresPrivados() throws Exception {
        Class<?>[] classes = {
                ClienteSpecification.class,
                CuentaSpecification.class,
                TransaccionServicioSpecification.class,
                UsuarioSpecification.class
        };
        for (Class<?> clazz : classes) {
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            assertNotNull(instance);
        }
    }
}
