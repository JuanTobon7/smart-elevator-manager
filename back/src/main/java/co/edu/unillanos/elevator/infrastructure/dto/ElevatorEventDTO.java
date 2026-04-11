package co.edu.unillanos.elevator.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un evento SSE de elevador
 * Se envía a través de Server-Sent Events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorEventDTO {
    private String elevatorId;
    private String eventType; // MOVING, ARRIVED, DOOR_OPENED, DOOR_CLOSED, ERROR, etc.
    private ElevatorStateDTO state;
    private String message;
    private long timestamp;
}
