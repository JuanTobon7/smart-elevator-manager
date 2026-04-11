package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Gestor de múltiples elevadores
 * Permite crear, obtener y listar elevadores
 */
@Component
public class ElevatorManager {
    private static final Logger log = LoggerFactory.getLogger(ElevatorManager.class);
    
    private final Map<String, Elevator> elevators = new ConcurrentHashMap<>();
    private final Map<String, ElevatorAsyncService> asyncServices = new ConcurrentHashMap<>();
    private final ElevatorEventBroadcaster eventBroadcaster;
    
    public ElevatorManager(ElevatorEventBroadcaster eventBroadcaster) {
        this.eventBroadcaster = eventBroadcaster;
    }
    
    /**
     * Obtiene o crea un elevador por ID
     */
    public Elevator getOrCreateElevator(String elevatorId) {
        return elevators.computeIfAbsent(elevatorId, id -> {
            log.info("Creando nuevo elevador con ID: {}", id);
            Elevator elevator = new Elevator();
            
            // Crear servicio asincrónico para este elevador
            ElevatorAsyncService asyncService = new ElevatorAsyncService(
                    elevator, 
                    id, 
                    eventBroadcaster
            );
            asyncServices.put(id, asyncService);
            
            return elevator;
        });
    }
    
    /**
     * Obtiene un elevador existente
     */
    public Elevator getElevator(String elevatorId) {
        Elevator elevator = elevators.get(elevatorId);
        if (elevator == null) {
            throw new IllegalArgumentException("Elevador no encontrado: " + elevatorId);
        }
        return elevator;
    }
    
    /**
     * Obtiene el servicio asincrónico de un elevador
     */
    public ElevatorAsyncService getAsyncService(String elevatorId) {
        ElevatorAsyncService service = asyncServices.get(elevatorId);
        if (service == null) {
            throw new IllegalArgumentException("Servicio de elevador no encontrado: " + elevatorId);
        }
        return service;
    }
    
    /**
     * Obtiene todos los elevadores
     */
    public Collection<Elevator> getAllElevators() {
        return elevators.values();
    }
    
    /**
     * Obtiene los estados de todos los elevadores
     */
    public Map<String, ElevatorStateDTO> getAllElevatorStates() {
        Map<String, ElevatorStateDTO> states = new LinkedHashMap<>();
        elevators.forEach((id, elevator) -> {
            states.put(id, ElevatorStateDTO.from(id, elevator));
        });
        return states;
    }
    
    /**
     * Obtiene el estado de un elevador específico
     */
    public ElevatorStateDTO getElevatorState(String elevatorId) {
        Elevator elevator = getElevator(elevatorId);
        return ElevatorStateDTO.from(elevatorId, elevator);
    }
    
    /**
     * Mueve un elevador a un piso específico de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> goToFloorAsync(String elevatorId, int targetFloor) {
        ElevatorAsyncService service = getAsyncService(elevatorId);
        return service.goToFloorAsync(targetFloor);
    }
    
    /**
     * Abre la puerta de un elevador de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> openDoorAsync(String elevatorId) {
        ElevatorAsyncService service = getAsyncService(elevatorId);
        return service.openDoorAsync();
    }
    
    /**
     * Cierra la puerta de un elevador de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> closeDoorAsync(String elevatorId) {
        ElevatorAsyncService service = getAsyncService(elevatorId);
        return service.closeDoorAsync();
    }
    
    /**
     * Reinicia un elevador de forma asincrónica
     */
    public CompletableFuture<ElevatorStateDTO> resetAsync(String elevatorId) {
        ElevatorAsyncService service = getAsyncService(elevatorId);
        return service.resetAsync();
    }
    
    /**
     * Obtiene el número total de elevadores
     */
    public int getElevatorCount() {
        return elevators.size();
    }
}
