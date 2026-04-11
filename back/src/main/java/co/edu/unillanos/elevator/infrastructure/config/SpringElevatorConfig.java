package co.edu.unillanos.elevator.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import co.edu.unillanos.elevator.infrastructure.elevator.ElevatorManager;

/**
 * Configuración de Spring para la aplicación de elevadores
 */
@Configuration
public class SpringElevatorConfig implements WebMvcConfigurer {
    
    /**
     * Bean para el broadcaster de eventos
     */
    @Bean
    public ElevatorEventBroadcaster elevatorEventBroadcaster() {
        return new ElevatorEventBroadcaster();
    }
    
    /**
     * Bean para el gestor de elevadores
     */
    @Bean
    public ElevatorManager elevatorManager(ElevatorEventBroadcaster eventBroadcaster) {
        return new ElevatorManager(eventBroadcaster);
    }
    
    /**
     * Bean para RestTemplate (si es necesario para llamadas HTTP)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * Configuración de CORS para permitir acceso desde frontend
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
