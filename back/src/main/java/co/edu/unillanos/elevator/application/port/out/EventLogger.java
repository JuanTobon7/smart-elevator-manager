package co.edu.unillanos.elevator.application.port.out;

import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Puerto de salida para logging de eventos del elevador
 */
public interface EventLogger {
    
    /**
     * Registra un evento en el log
     */
    void logEvent(String eventType, String details, SensorReading reading);

    /**
     * Registra un error
     */
    void logError(String errorMessage);
}
