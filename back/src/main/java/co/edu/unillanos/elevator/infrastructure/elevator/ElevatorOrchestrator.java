package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Orquesta el ciclo de vida y las operaciones de un elevador concreto.
 * Centraliza la coordinación entre dominio, hardware y eventos.
 */
public class ElevatorOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(ElevatorOrchestrator.class);

    private final Elevator elevator;
    private final String elevatorId;
    private final HardwarePort hardwarePort;
    private final String backendType;
    private final ElevatorEventBroadcaster eventBroadcaster;
    private final Object lock = new Object();

    public ElevatorOrchestrator(
            Elevator elevator,
            String elevatorId,
            HardwarePort hardwarePort,
            String backendType,
            ElevatorEventBroadcaster eventBroadcaster
    ) {
        this.elevator = elevator;
        this.elevatorId = elevatorId;
        this.hardwarePort = hardwarePort;
        this.backendType = backendType;
        this.eventBroadcaster = eventBroadcaster;
    }

    public Elevator getElevator() {
        return elevator;
    }

    public ElevatorStateDTO getCurrentState() {
        synchronized (lock) {
            return ElevatorStateDTO.from(elevatorId, elevator);
        }
    }

    public String getBackendType() {
        return backendType;
    }

    public CompletableFuture<ElevatorStateDTO> goToFloorAsync(int targetFloor) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    if (elevator.getCurrentFloor() == targetFloor) {
                        log.info("[{}] Ya está en el piso {} usando backend {}", elevatorId, targetFloor, backendType);
                        return ElevatorStateDTO.from(elevatorId, elevator);
                    }

                    log.info("[{}] Iniciando movimiento al piso {} usando backend {}", elevatorId, targetFloor, backendType);

                    elevator.goToFloor(targetFloor);
                    broadcast("MOVING", "Elevador en movimiento al piso " + targetFloor);

                    hardwarePort.moveToFloor(targetFloor);
                    elevator.arriveAtFloor();

                    broadcast("ARRIVED", "Elevador llegó al piso " + targetFloor);
                    return ElevatorStateDTO.from(elevatorId, elevator);
                } catch (ElevatorException e) {
                    log.error("[{}] Error de dominio moviendo elevador: {}", elevatorId, e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                } catch (RuntimeException e) {
                    log.error("[{}] Error de hardware moviendo elevador: {}", elevatorId, e.getMessage(), e);
                    elevator.setError(e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                }
            }
        });
    }

    public CompletableFuture<ElevatorStateDTO> openDoorAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    log.info("[{}] Abriendo puerta usando backend {}", elevatorId, backendType);

                    elevator.openDoor();
                    hardwarePort.openDoor();

                    broadcast("DOOR_OPENED", "Puerta abierta");
                    return ElevatorStateDTO.from(elevatorId, elevator);
                } catch (ElevatorException e) {
                    log.error("[{}] Error de dominio abriendo puerta: {}", elevatorId, e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                } catch (RuntimeException e) {
                    log.error("[{}] Error de hardware abriendo puerta: {}", elevatorId, e.getMessage(), e);
                    elevator.setError(e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                }
            }
        });
    }

    public CompletableFuture<ElevatorStateDTO> closeDoorAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    log.info("[{}] Cerrando puerta usando backend {}", elevatorId, backendType);

                    elevator.closeDoor();
                    hardwarePort.closeDoor();
                    elevator.completeDoorClosing();

                    broadcast("DOOR_CLOSED", "Puerta cerrada");
                    return ElevatorStateDTO.from(elevatorId, elevator);
                } catch (ElevatorException e) {
                    log.error("[{}] Error de dominio cerrando puerta: {}", elevatorId, e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                } catch (RuntimeException e) {
                    log.error("[{}] Error de hardware cerrando puerta: {}", elevatorId, e.getMessage(), e);
                    elevator.setError(e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                }
            }
        });
    }

    public CompletableFuture<ElevatorStateDTO> resetAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    log.info("[{}] Reiniciando elevador usando backend {}", elevatorId, backendType);

                    hardwarePort.reset();
                    elevator.reset();

                    broadcast("RESET", "Elevador reiniciado");
                    return ElevatorStateDTO.from(elevatorId, elevator);
                } catch (RuntimeException e) {
                    log.error("[{}] Error reiniciando elevador: {}", elevatorId, e.getMessage(), e);
                    elevator.setError(e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                }
            }
        });
    }

    private void emitErrorEvent(String message) {
        broadcast("ERROR", message);
    }

    private void broadcast(String eventType, String message) {
        ElevatorEventDTO event = ElevatorEventDTO.builder()
                .elevatorId(elevatorId)
                .eventType(eventType)
                .state(ElevatorStateDTO.from(elevatorId, elevator))
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
        eventBroadcaster.broadcastToElevator(elevatorId, event);
    }
}
