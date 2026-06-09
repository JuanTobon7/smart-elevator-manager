package co.edu.unillanos.elevator.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;

/**
 * DTO que representa el estado actual de un elevador.
 * Se usa en endpoints REST y eventos SSE.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorStateDTO {

    private String elevatorId;
    private int    currentFloor;
    private int    targetFloor;

    /** IDLE | GOING_UP | GOING_DOWN | DOOR_OPEN | DOOR_OPENING | DOOR_CLOSING | EMERGENCY_STOP | ERROR */
    private String status;

    /** UP | DOWN | NONE */
    private String direction;

    /** OPEN | CLOSED | CLOSING */
    private String doorStatus;

    /** true cuando status es GOING_UP o GOING_DOWN — comodín para el frontend */
    private boolean moving;

    /** true cuando status es EMERGENCY_STOP */
    private boolean emergency;

    private long timestamp;

    /**
     * Convierte un Elevator a ElevatorStateDTO.
     */
    public static ElevatorStateDTO from(String elevatorId, Elevator elevator) {
        ElevatorState state = elevator.getState();

        return ElevatorStateDTO.builder()
                .elevatorId(elevatorId)
                .currentFloor(elevator.getCurrentFloor())
                .targetFloor(elevator.getTargetFloor())
                .status(state.toString())
                .direction(elevator.getDirection().toString())
                .doorStatus(elevator.getDoor().getState().toString())
                // ← NUEVO: flags derivados para que el frontend no tenga que comparar strings
                .moving(state == ElevatorState.GOING_UP || state == ElevatorState.GOING_DOWN)
                .emergency(state == ElevatorState.EMERGENCY_STOP)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}