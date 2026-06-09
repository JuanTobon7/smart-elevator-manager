// ElevatorOrchestrator.java
package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.domain.model.SensorReading;
import co.edu.unillanos.elevator.infrastructure.adapter.ArduinoAdapter;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

    public Elevator getElevator() { return elevator; }

    public ElevatorStateDTO getCurrentState() {
        synchronized (lock) {
            return ElevatorStateDTO.from(elevatorId, elevator);
        }
    }

    public String getBackendType() { return backendType; }

    // ── MODIFICADO: sincroniza dominio con hardware antes de moverse ──
    public CompletableFuture<ElevatorStateDTO> goToFloorAsync(int targetFloor) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    // 1. Sincronizar estado del dominio con el hardware real
                    syncDomainWithHardware();

                    if (elevator.getCurrentFloor() == targetFloor) {
                        log.info("[{}] Ya está en el piso {}", elevatorId, targetFloor);
                        return ElevatorStateDTO.from(elevatorId, elevator);
                    }

                    log.info("[{}] Iniciando movimiento al piso {} usando backend {}",
                             elevatorId, targetFloor, backendType);

                    // 2. Transición de dominio (valida reglas de negocio)
                    elevator.goToFloor(targetFloor);
                    broadcast("MOVING", "Elevador en movimiento al piso " + targetFloor);

                    // 3. Ejecutar en hardware — bloquea hasta llegar o timeout
                    hardwarePort.moveToFloor(targetFloor);

                    // Pequeña pausa para que el Arduino estabilice el estado final
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // restaurar flag de interrupción
                        log.warn("[{}] Sleep interrumpido esperando estabilización del hardware", elevatorId);
                    }


                    // 4. Verificar llegada
                    SensorReading finalState = syncDomainWithHardware();

                    if (finalState.getFloor() == targetFloor
                        && (finalState.getElevatorState() == ElevatorState.IDLE
                            || finalState.getElevatorState() == ElevatorState.GOING_UP   // por si acaba de parar
                            || finalState.getElevatorState() == ElevatorState.GOING_DOWN)) {
                        elevator.arriveAtFloor();
                        broadcast("ARRIVED", "Elevador llegó al piso " + targetFloor);
                        log.info("[{}] Elevador llegó al piso {}", elevatorId, targetFloor);
                    } else {
                        // El hardware reporta un piso distinto — timeout real
                        String msg = String.format(
                            "Error: se esperaba piso %d, hardware reporta piso %d (estado: %s)",
                            targetFloor, finalState.getFloor(), finalState.getElevatorState()
                        );
                        log.warn("[{}] {}", elevatorId, msg);
                        elevator.setError(msg);
                        emitErrorEvent(msg);
                        throw new CompletionException(new RuntimeException(msg));
                    }

                    return ElevatorStateDTO.from(elevatorId, elevator);

                } catch (ElevatorException e) {
                    log.error("[{}] Error de dominio moviendo elevador: {}", elevatorId, e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                } catch (IllegalStateException e) {
                    log.warn("[{}] Condición no cumplida: {}", elevatorId, e.getMessage());
                    broadcast("VALIDATION_ERROR", e.getMessage());
                    throw new CompletionException(e);
                } catch (CompletionException e) {
                    throw e; // ya fue manejada arriba
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

                    // 1. Sincronizar antes de validar
                    syncDomainWithHardware();

                    // 2. Transición de dominio
                    elevator.openDoor();

                    // 3. Ejecutar en hardware
                    hardwarePort.openDoor();

                    // 4. Confirmar con sensor
                    syncDomainWithHardware();

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

    // ── MODIFICADO: sincronizar estado real antes de intentar cerrar ──
    public CompletableFuture<ElevatorStateDTO> closeDoorAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    log.info("[{}] Cerrando puerta usando backend {}", elevatorId, backendType);

                    // 1. Leer estado real del hardware
                    SensorReading current = syncDomainWithHardware();

                    // 2. Validar que la puerta realmente está abierta según el sensor
                    if (current.getDoorState() != DoorState.OPEN) {
                        String msg = "La puerta no está abierta según el sensor (estado: "
                                     + current.getDoorState() + ")";
                        log.warn("[{}] {}", elevatorId, msg);
                        broadcast("VALIDATION_ERROR", msg);
                        throw new CompletionException(new ElevatorException(msg));
                    }

                    // 3. Transición de dominio (ahora ya está sincronizado → no fallará)
                    elevator.closeDoor();

                    // 4. Ejecutar en hardware
                    hardwarePort.closeDoor();

                    // 5. Completar transición de dominio
                    elevator.completeDoorClosing();

                    // 6. Confirmar con sensor
                    syncDomainWithHardware();

                    broadcast("DOOR_CLOSED", "Puerta cerrada");
                    return ElevatorStateDTO.from(elevatorId, elevator);

                } catch (ElevatorException e) {
                    log.error("[{}] Error de dominio cerrando puerta: {}", elevatorId, e.getMessage());
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                } catch (CompletionException e) {
                    throw e;
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

    
    public CompletableFuture<ElevatorStateDTO> emergencyStopAsync() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                try {
                    log.warn("[{}] Ejecutando parada de emergencia", elevatorId);

                    // Enviar al hardware inmediatamente — no esperar sincronización
                    hardwarePort.emergencyStop();

                    // Actualizar dominio
                    elevator.setEmergencyStop();

                    broadcast("EMERGENCY_STOP", "Parada de emergencia activada");
                    log.warn("[{}] Parada de emergencia confirmada", elevatorId);

                    return ElevatorStateDTO.from(elevatorId, elevator);

                } catch (RuntimeException e) {
                    log.error("[{}] Error ejecutando parada de emergencia: {}", elevatorId, e.getMessage(), e);
                    emitErrorEvent(e.getMessage());
                    throw new CompletionException(e);
                }
            }
        });
    }


    private SensorReading syncDomainWithHardware() {
        try {
            SensorReading reading = hardwarePort.readState();

            // Sincronizar piso
            int hwFloor = reading.getFloor();
            if (hwFloor != elevator.getCurrentFloor()) {
                log.info("[{}] Sincronizando piso: dominio={} hardware={}",
                         elevatorId, elevator.getCurrentFloor(), hwFloor);
                // Forzar piso vía reset parcial sin tocar otros estados
                elevator.syncFloor(hwFloor);
            }

            // Sincronizar puerta
            DoorState hwDoor = reading.getDoorState();
            ElevatorState domainState = elevator.getState();

            if (hwDoor == DoorState.OPEN && domainState == ElevatorState.IDLE) {
                // Hardware dice puerta abierta pero dominio cree que está cerrado en IDLE
                log.info("[{}] Sincronizando puerta: hardware=OPEN, forzando DOOR_OPEN en dominio",
                         elevatorId);
                elevator.forceDoorOpen();
            } else if (hwDoor == DoorState.CLOSED
                    && (domainState == ElevatorState.DOOR_OPEN
                        || domainState == ElevatorState.DOOR_CLOSING)) {
                // Hardware dice puerta cerrada pero dominio cree que sigue abierta
                log.info("[{}] Sincronizando puerta: hardware=CLOSED, forzando IDLE en dominio",
                         elevatorId);
                elevator.forceDoorClosed();
            }

            return reading;

        } catch (Exception e) {
            log.warn("[{}] No se pudo sincronizar con hardware: {}", elevatorId, e.getMessage());
            // Retornar lectura desde dominio como fallback
            return elevator.readSensors();
        }
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