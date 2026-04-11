package co.edu.unillanos.elevator.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.application.port.in.ElevatorUseCase;
import co.edu.unillanos.elevator.application.port.out.EventLogger;
import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Servicio de aplicación que implementa los casos de uso del elevador
 */
@Service
@RequiredArgsConstructor
public class ElevatorService implements ElevatorUseCase {

    private static final Logger log = LoggerFactory.getLogger(ElevatorService.class);
    private final Elevator elevator;
    private final HardwarePort hardwarePort;
    private final EventLogger eventLogger;

    @Override
    public void goToFloor(int floor) {
        try {
            log.info("Comando: GO {}", floor);
            
            // Delegar al dominio para validar reglas
            elevator.goToFloor(floor);
            
            // Ejecutar el movimiento en hardware
            hardwarePort.moveToFloor(floor);
            
            // Simular llegada
            Thread.sleep(500 * Math.abs(elevator.getTargetFloor() - elevator.getCurrentFloor()));
            elevator.arriveAtFloor();
            
            SensorReading reading = elevator.readSensors();
            eventLogger.logEvent("ARRIVED", "floor=" + reading.getFloor(), reading);
            log.info("[OK] IDLE | Floor: {} | Door: {}", reading.getFloor(), reading.getDoorState());
            
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

    @Override
    public SensorReading readSensors() {
        try {
            SensorReading reading = hardwarePort.readState();
            log.info("READ | Floor: {} | Door: {} | State: {}", 
                reading.getFloor(), reading.getDoorState(), reading.getElevatorState());
            return reading;
        } catch (InterruptedException e) {
            log.error("[ERROR] {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new ElevatorException("Error al leer sensores: " + e.getMessage());
        }
    }

    @Override
    public void reset() {
        try {
            log.info("Comando: RESET");
            
            elevator.reset();
            hardwarePort.reset();
            
            SensorReading reading = elevator.readSensors();
            eventLogger.logEvent("RESET", "floor=1 | state=IDLE | door=CLOSED", reading);
            log.info("[OK] IDLE | Floor: 1 | Door: CLOSED");
            
        } catch (Exception e) {
            log.error("[ERROR] {}", e.getMessage());
            eventLogger.logError(e.getMessage());
        }
    }
}
