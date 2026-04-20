package co.edu.unillanos.elevator.infrastructure.adapter;

import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador para simular el hardware del elevador sin Arduino físico.
 */
@Component
@Profile("simulator")
public class SimulatorAdapter implements HardwarePort {

    private static final Logger log = LoggerFactory.getLogger(SimulatorAdapter.class);

    private int currentFloor = 1;
    private DoorState doorState = DoorState.CLOSED;
    private ElevatorState elevatorState = ElevatorState.IDLE;
    private int targetFloor = 1;

    @Override
    public void executeCommand(String command) {
        log.debug("Simulador ejecutando comando: {}", command);

        String[] parts = command.split("\\s+");
        String cmd = parts[0].toUpperCase();

        switch (cmd) {
            case "GO":
                if (parts.length > 1) {
                    int floor = Integer.parseInt(parts[1]);
                    moveToFloor(floor);
                }
                break;
            case "DOOR":
                if (parts.length > 1 && "OPEN".equalsIgnoreCase(parts[1])) {
                    openDoor();
                } else if (parts.length > 1 && "CLOSE".equalsIgnoreCase(parts[1])) {
                    closeDoor();
                }
                break;
            case "READ":
                try {
                    SensorReading reading = readState();
                    log.info("Lectura: Floor={}, Door={}, State={}",
                            reading.getFloor(), reading.getDoorState(), reading.getElevatorState());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;
            case "RESET":
                reset();
                break;
            default:
                log.warn("Comando desconocido: {}", cmd);
        }
    }

    @Override
    public SensorReading readState() throws InterruptedException {
        return SensorReading.builder()
                .floor(currentFloor)
                .doorState(doorState)
                .elevatorState(elevatorState)
                .build();
    }

    @Override
    public void moveToFloor(int floor) {
        log.info("Simulador: moviendo a piso {}", floor);

        targetFloor = floor;
        elevatorState = ElevatorState.MOVING;

        int distance = Math.abs(floor - currentFloor);
        try {
            Thread.sleep(500L * distance);
        } catch (InterruptedException e) {
            log.warn("Movimiento interrumpido");
            Thread.currentThread().interrupt();
        }

        currentFloor = floor;
        elevatorState = ElevatorState.IDLE;
        log.info("Simulador: llegada a piso {}", floor);
    }

    @Override
    public void openDoor() {
        log.info("Simulador: abriendo puerta");
        doorState = DoorState.OPEN;
        elevatorState = ElevatorState.DOOR_OPEN;
    }

    @Override
    public void closeDoor() {
        log.info("Simulador: cerrando puerta");
        doorState = DoorState.CLOSING;
        elevatorState = ElevatorState.DOOR_CLOSING;

        try {
            Thread.sleep(800L);
        } catch (InterruptedException e) {
            log.warn("Cierre de puerta interrumpido");
            Thread.currentThread().interrupt();
        }

        doorState = DoorState.CLOSED;
        elevatorState = ElevatorState.IDLE;
        log.info("Simulador: puerta cerrada");
    }

    @Override
    public void reset() {
        log.info("Simulador: reset a estado inicial");

        currentFloor = 1;
        targetFloor = 1;
        doorState = DoorState.CLOSED;
        elevatorState = ElevatorState.IDLE;
    }
}
