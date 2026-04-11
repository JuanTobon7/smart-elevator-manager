package co.edu.unillanos.elevator.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import co.edu.unillanos.elevator.domain.model.Elevator;

/**
 * DTO que representa el estado actual de un elevador
 * Se usa en endpoints REST y eventos SSE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorStateDTO {
    private String elevatorId;
    private int currentFloor;
    private int targetFloor;
    private String status; // IDLE, MOVING, DOOR_OPEN, DOOR_CLOSING, ERROR
    private String direction; // UP, DOWN, NONE
    private String doorStatus; // OPEN, CLOSED, CLOSING
    private long timestamp;
    
    /**
     * Convierte un Elevator a ElevatorStateDTO
     */
    public static ElevatorStateDTO from(String elevatorId, Elevator elevator) {
        return ElevatorStateDTO.builder()
                .elevatorId(elevatorId)
                .currentFloor(elevator.getCurrentFloor())
                .targetFloor(elevator.getTargetFloor())
                .status(elevator.getState().toString())
                .direction(elevator.getDirection().toString())
                .doorStatus(elevator.getDoor().getState().toString())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
