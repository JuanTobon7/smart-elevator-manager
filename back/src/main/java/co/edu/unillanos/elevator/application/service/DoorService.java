package co.edu.unillanos.elevator.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.application.port.in.DoorUseCase;
import co.edu.unillanos.elevator.application.port.out.EventLogger;
import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Servicio de aplicación para casos de uso relacionados con la puerta
 */
@Service
@RequiredArgsConstructor
public class DoorService implements DoorUseCase {

    private static final Logger log = LoggerFactory.getLogger(DoorService.class);
    private final Elevator elevator;
    private final HardwarePort hardwarePort;
    private final EventLogger eventLogger;

    @Override
    public void open() {
        try {
            log.info("Comando: DOOR OPEN");
            
            // Validar y cambiar estado en dominio
            elevator.openDoor();
            
            // Ejecutar en hardware
            hardwarePort.openDoor();
            
            SensorReading reading = elevator.readSensors();
            eventLogger.logEvent("OPEN", "floor=" + reading.getFloor(), reading);
            log.info("[OK] DOOR_OPEN | Floor: {} | Door: OPEN", reading.getFloor());
            
        } catch (ElevatorException e) {
            log.error("[ERROR] {}", e.getMessage());
            eventLogger.logError(e.getMessage());
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            log.info("Comando: DOOR CLOSE");
            
            // Iniciar cierre en dominio
            elevator.closeDoor();
            
            // Ejecutar cierre en hardware (simula tiempo de cierre)
            hardwarePort.closeDoor();
            
            // Simular tiempo de cierre
            Thread.sleep(800);
            
            // Completar cierre
            elevator.completeDoorClosing();
            
            SensorReading reading = elevator.readSensors();
            eventLogger.logEvent("CLOSE", "floor=" + reading.getFloor(), reading);
            log.info("[OK] IDLE | Floor: {} | Door: CLOSED", reading.getFloor());
            
        } catch (ElevatorException e) {
            log.error("[ERROR] {}", e.getMessage());
            eventLogger.logError(e.getMessage());
            throw e;
        } catch (InterruptedException e) {
            log.error("[ERROR] {}", e.getMessage());
            eventLogger.logError(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
