package co.edu.unillanos.elevator.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import co.edu.unillanos.elevator.domain.model.Elevator;

/**
 * Configuración de Spring Boot para wiring de dependencias
 */
@Configuration
public class ElevatorConfig {

    /**
     * Crea el bean de dominio Elevator (sin dependencias de Spring)
     */
    @Bean
    public Elevator elevator() {
        return new Elevator();
    }
}
