package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Servicio asincrónico para un elevador específico
 * Envuelve las operaciones del elevador en CompletableFuture
 */
public class ElevatorAsyncService {
    private static final Logger log = LoggerFactory.getLogger(ElevatorAsyncService.class);
    
    private final Elevator elevator;
    private final String elevatorId;
    private final ElevatorEventBroadcaster eventBroadcaster;
    
    public ElevatorAsyncService(Elevator elevator, String elevatorId, ElevatorEventBroadcaster eventBroadcaster) {
        this.elevator = elevator;
        this.elevatorId = elevatorId;
        this.eventBroadcaster = eventBroadcaster;
    }
    
    /**
     * Mueve el elevador a un piso de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> goToFloorAsync(int targetFloor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("[{}] Iniciando movimiento al piso: {}", elevatorId, targetFloor);
                
                // Validar y cambiar estado
                elevator.goToFloor(targetFloor);
                
                // Emitir evento de inicio de movimiento
                ElevatorEventDTO event = ElevatorEventDTO.builder()
                        .elevatorId(elevatorId)
                        .eventType("MOVING")
                        .state(ElevatorStateDTO.from(elevatorId, elevator))
                        .message("Elevador en movimiento al piso " + targetFloor)
                        .timestamp(System.currentTimeMillis())
                        .build();
                eventBroadcaster.broadcastToElevator(elevatorId, event);
                
                // Simular tiempo de movimiento
                long travelTime = 1000L * Math.abs(elevator.getTargetFloor() - elevator.getCurrentFloor());
                Thread.sleep(travelTime);
                
                // Llegada
                elevator.arriveAtFloor();
                
                // Emitir evento de llegada
                event = ElevatorEventDTO.builder()
                        .elevatorId(elevatorId)
                        .eventType("ARRIVED")
                        .state(ElevatorStateDTO.from(elevatorId, elevator))
                        .message("Elevador llegó al piso " + targetFloor)
                        .timestamp(System.currentTimeMillis())
                        .build();
                eventBroadcaster.broadcastToElevator(elevatorId, event);
                
                log.info("[{}] Llegó al piso: {}", elevatorId, targetFloor);
                return ElevatorStateDTO.from(elevatorId, elevator);
                
            } catch (ElevatorException e) {
                log.error("[{}] Error en elevador: {}", elevatorId, e.getMessage());
                emitErrorEvent(e.getMessage());
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                log.error("[{}] Interrumpido: {}", elevatorId, e.getMessage());
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Abre la puerta de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> openDoorAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("[{}] Abriendo puerta", elevatorId);
                
                elevator.openDoor();
                
                ElevatorEventDTO event = ElevatorEventDTO.builder()
                        .elevatorId(elevatorId)
                        .eventType("DOOR_OPENED")
                        .state(ElevatorStateDTO.from(elevatorId, elevator))
                        .message("Puerta abierta")
                        .timestamp(System.currentTimeMillis())
                        .build();
                eventBroadcaster.broadcastToElevator(elevatorId, event);
                
                log.info("[{}] Puerta abierta", elevatorId);
                return ElevatorStateDTO.from(elevatorId, elevator);
                
            } catch (ElevatorException e) {
                log.error("[{}] Error al abrir puerta: {}", elevatorId, e.getMessage());
                emitErrorEvent(e.getMessage());
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Cierra la puerta de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> closeDoorAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("[{}] Cerrando puerta", elevatorId);
                
                elevator.closeDoor();
                
                // Simular tiempo de cierre
                Thread.sleep(1000);
                
                elevator.completeDoorClosing();
                
                ElevatorEventDTO event = ElevatorEventDTO.builder()
                        .elevatorId(elevatorId)
                        .eventType("DOOR_CLOSED")
                        .state(ElevatorStateDTO.from(elevatorId, elevator))
                        .message("Puerta cerrada")
                        .timestamp(System.currentTimeMillis())
                        .build();
                eventBroadcaster.broadcastToElevator(elevatorId, event);
                
                log.info("[{}] Puerta cerrada", elevatorId);
                return ElevatorStateDTO.from(elevatorId, elevator);
                
            } catch (ElevatorException e) {
                log.error("[{}] Error al cerrar puerta: {}", elevatorId, e.getMessage());
                emitErrorEvent(e.getMessage());
                throw new CompletionException(e);
            } catch (InterruptedException e) {
                log.error("[{}] Interrumpido: {}", elevatorId, e.getMessage());
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Reinicia el elevador de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> resetAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("[{}] Reiniciando elevador", elevatorId);
                
                elevator.reset();
                
                ElevatorEventDTO event = ElevatorEventDTO.builder()
                        .elevatorId(elevatorId)
                        .eventType("RESET")
                        .state(ElevatorStateDTO.from(elevatorId, elevator))
                        .message("Elevador reiniciado")
                        .timestamp(System.currentTimeMillis())
                        .build();
                eventBroadcaster.broadcastToElevator(elevatorId, event);
                
                log.info("[{}] Elevador reiniciado", elevatorId);
                return ElevatorStateDTO.from(elevatorId, elevator);
                
            } catch (Exception e) {
                log.error("[{}] Error al reiniciar: {}", elevatorId, e.getMessage());
                emitErrorEvent(e.getMessage());
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Emite un evento de error
     */
    private void emitErrorEvent(String message) {
        ElevatorEventDTO event = ElevatorEventDTO.builder()
                .elevatorId(elevatorId)
                .eventType("ERROR")
                .state(ElevatorStateDTO.from(elevatorId, elevator))
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
        eventBroadcaster.broadcastToElevator(elevatorId, event);
    }
}
