package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de múltiples elevadores.
 * Mantiene el registro de elevadores y delega su creación a una factoría/orquestación dedicada.
 */
@Component
public class ElevatorManager {
    private static final Logger log = LoggerFactory.getLogger(ElevatorManager.class);

    private final Map<String, ElevatorOrchestrator> orchestrators = new ConcurrentHashMap<>();
    private final ElevatorOrchestratorFactory orchestratorFactory;

    public ElevatorManager(ElevatorOrchestratorFactory orchestratorFactory) {
        this.orchestratorFactory = orchestratorFactory;
    }

    /**
     * Obtiene o crea un elevador por ID.
     */
    public Elevator getOrCreateElevator(String elevatorId) {
        return getOrCreateOrchestrator(elevatorId).getElevator();
    }

    /**
     * Obtiene un elevador existente.
     */
    public Elevator getElevator(String elevatorId) {
        ElevatorOrchestrator orchestrator = orchestrators.get(elevatorId);
        if (orchestrator == null) {
            throw new IllegalArgumentException("Elevador no encontrado: " + elevatorId);
        }
        return orchestrator.getElevator();
    }

    /**
     * Obtiene todos los elevadores.
     */
    public Collection<Elevator> getAllElevators() {
        List<Elevator> elevators = new ArrayList<>();
        orchestrators.values().forEach(orchestrator -> elevators.add(orchestrator.getElevator()));
        return elevators;
    }

    /**
     * Obtiene los estados de todos los elevadores.
     */
    public Map<String, ElevatorStateDTO> getAllElevatorStates() {
        Map<String, ElevatorStateDTO> states = new LinkedHashMap<>();
        orchestrators.forEach((id, orchestrator) -> states.put(id, orchestrator.getCurrentState()));
        return states;
    }

    /**
     * Obtiene el estado de un elevador específico. Si no existe, lo crea automáticamente.
     */
    public ElevatorStateDTO getElevatorState(String elevatorId) {
        return getOrCreateOrchestrator(elevatorId).getCurrentState();
    }

    /**
     * Mueve un elevador a un piso específico de forma asíncrona.
     */
    public CompletableFuture<ElevatorStateDTO> goToFloorAsync(String elevatorId, int targetFloor) {
        return getOrCreateOrchestrator(elevatorId).goToFloorAsync(targetFloor);
    }

    /**
     * Abre la puerta de un elevador de forma asíncrona.
     */
    public CompletableFuture<ElevatorStateDTO> openDoorAsync(String elevatorId) {
        return getOrCreateOrchestrator(elevatorId).openDoorAsync();
    }

    /**
     * Cierra la puerta de un elevador de forma asíncrona.
     */
    public CompletableFuture<ElevatorStateDTO> closeDoorAsync(String elevatorId) {
        return getOrCreateOrchestrator(elevatorId).closeDoorAsync();
    }

    /**
     * Reinicia un elevador de forma asíncrona.
     */
    public CompletableFuture<ElevatorStateDTO> resetAsync(String elevatorId) {
        return getOrCreateOrchestrator(elevatorId).resetAsync();
    }

    /**
     * Obtiene el número total de elevadores.
     */
    public int getElevatorCount() {
        return orchestrators.size();
    }

    private ElevatorOrchestrator getOrCreateOrchestrator(String elevatorId) {
        return orchestrators.computeIfAbsent(elevatorId, id -> {
            log.info("Creando nuevo elevador con ID: {}", id);
            return orchestratorFactory.create(id);
        });
    }
}
