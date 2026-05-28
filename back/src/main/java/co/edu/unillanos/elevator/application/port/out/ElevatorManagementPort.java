package co.edu.unillanos.elevator.application.port.out;

import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Puerto de salida para la gestion de elevadores.
 * Define el contrato para las operaciones de elevadores.
 */
public interface ElevatorManagementPort {
    
    Elevator getOrCreateElevator(String elevatorId) throws ElevatorException;
    
    Elevator getElevator(String elevatorId) throws ElevatorException;
    
    Collection<Elevator> getAllElevators();
    
    Map<String, ElevatorStateDTO> getAllElevatorStates();
    
    ElevatorStateDTO getElevatorState(String elevatorId) throws ElevatorException;
    
    CompletableFuture<ElevatorStateDTO> goToFloorAsync(String elevatorId, int targetFloor);
    
    CompletableFuture<ElevatorStateDTO> openDoorAsync(String elevatorId);
    
    CompletableFuture<ElevatorStateDTO> closeDoorAsync(String elevatorId);
    
    CompletableFuture<ElevatorStateDTO> resetAsync(String elevatorId);
    
    int getElevatorCount();
}