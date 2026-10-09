package com.proyecto.servicios.util;

import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class GeneradorCuentaUtil {

    private static final String PREFIJO_BANCO = "4152"; // Prefijo estándar de 4 dígitos
    private final CuentaRepository cuentaRepository;

    public GeneradorCuentaUtil(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Genera un número de cuenta único de 16 dígitos.
     * Garantiza colisión cero incluso bajo pruebas masivas de 100,000+ cuentas.
     */
    public String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            long randomDigits = ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L);
            numeroCuenta = PREFIJO_BANCO + randomDigits;
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));

        return numeroCuenta;
    }
}
