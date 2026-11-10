package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceTest {

    @Mock
    private GestoPagoAuthClient authClient;

    @Mock
    private GestoPagoTokenRepository tokenRepository;

    @Mock
    private GestoPagoTokenMapper tokenMapper;

    @InjectMocks
    private GestoPagoTokenService tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "idDistribuidor", 83);
        ReflectionTestUtils.setField(tokenService, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(tokenService, "password", "12345678");
    }

    @Test
    @DisplayName("Debe renovar token insertando nuevo registro cuando no existe previo")
    void testRenovarTokenNuevo() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("jwt-token-123");

        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(response);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());
        when(tokenMapper.toEntity(response)).thenReturn(new GestoPagoToken());

        tokenService.renovarToken();

        verify(tokenRepository, times(1)).save(any(GestoPagoToken.class));
    }

    @Test
    @DisplayName("Debe actualizar token existente cuando ya existe en BD")
    void testRenovarTokenExistente() {
        GestoPagoAuthResponse response = new GestoPagoAuthResponse();
        response.setToken("jwt-token-nuevo");

        GestoPagoToken existente = new GestoPagoToken();
        existente.setId(1);

        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(response);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(existente));

        tokenService.renovarToken();

        verify(tokenMapper, times(1)).updateEntity(response, existente);
        verify(tokenRepository, times(1)).save(existente);
    }

    @Test
    @DisplayName("Debe manejar respuesta nula o sin token sin guardar en BD")
    void testRenovarTokenRespuestaNula() {
        when(authClient.authenticate(anyInt(), anyString(), anyString())).thenReturn(null);

        tokenService.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar excepción en cliente Feign sin propagar error no controlado")
    void testRenovarTokenExcepcion() {
        when(authClient.authenticate(anyInt(), anyString(), anyString())).thenThrow(new RuntimeException("Error HTTP 500"));

        assertDoesNotThrow(() -> tokenService.renovarToken());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe obtener token activo del repositorio")
    void testObtenerTokenActivo() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token-activo");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "dev-01")).thenReturn(Optional.of(token));

        Optional<GestoPagoToken> resultado = tokenService.obtenerTokenActivo(83, "dev-01");
        assertTrue(resultado.isPresent());
        assertEquals("token-activo", resultado.get().getToken());
    }
}
