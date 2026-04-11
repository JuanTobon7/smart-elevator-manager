package co.edu.unillanos.elevator.domain.model;

import lombok.Data;
import lombok.Builder;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;

/**
 * Lectura de sensores del elevador
 */
@Data
@Builder
public class SensorReading {
    private final int floor;
    private final DoorState doorState;
    private final ElevatorState elevatorState;
}
