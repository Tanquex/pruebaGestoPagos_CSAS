package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BCryptConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Fuerza 10: estándar de seguridad para BCrypt optimizado para alto rendimiento
        return new BCryptPasswordEncoder(10);
    }
}
