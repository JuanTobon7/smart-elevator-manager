package co.edu.unillanos.elevator.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Representa un piso en el edificio
 */
@Getter
@Setter
public class Floor {
    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 5;
    
    private final int number;

    public Floor(int number) {
        if (number < MIN_FLOOR || number > MAX_FLOOR) {
            throw new IllegalArgumentException(
                String.format("Piso inválido: %d (rango válido: %d-%d)", number, MIN_FLOOR, MAX_FLOOR)
            );
        }
        this.number = number;
    }

    public static boolean isValid(int floorNumber) {
        return floorNumber >= MIN_FLOOR && floorNumber <= MAX_FLOOR;
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }
}
