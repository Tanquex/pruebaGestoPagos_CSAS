package com.proyecto.servicios.util;

import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneradorCuentaUtilTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private GeneradorCuentaUtil generadorCuentaUtil;

    @Test
    @DisplayName("Debe generar número de cuenta de 16 dígitos que empiece con 4152")
    void testGenerarNumeroCuentaUnico() {
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        String cuenta = generadorCuentaUtil.generarNumeroCuentaUnico();

        assertNotNull(cuenta);
        assertEquals(16, cuenta.length());
        assertTrue(cuenta.startsWith("4152"));
        verify(cuentaRepository, atLeastOnce()).existsByNumeroCuenta(cuenta);
    }

    @Test
    @DisplayName("Debe reintentar si el primer número generado ya existe")
    void testReintentoSiExiste() {
        when(cuentaRepository.existsByNumeroCuenta(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String cuenta = generadorCuentaUtil.generarNumeroCuentaUnico();

        assertNotNull(cuenta);
        assertEquals(16, cuenta.length());
        verify(cuentaRepository, times(2)).existsByNumeroCuenta(anyString());
    }
}

