package co.edu.unillanos.elevator.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar movimiento a un piso
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestFloorDTO {
    private int floor;
}
