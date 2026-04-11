package co.edu.unillanos.elevator.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Representa la puerta del elevador
 */
@Getter
@Setter
public class Door {
    private co.edu.unillanos.elevator.domain.enums.DoorState state;

    public Door() {
        this.state = co.edu.unillanos.elevator.domain.enums.DoorState.CLOSED;
    }

    public void open() {
        this.state = co.edu.unillanos.elevator.domain.enums.DoorState.OPEN;
    }

    public void startClosing() {
        this.state = co.edu.unillanos.elevator.domain.enums.DoorState.CLOSING;
    }

    public void close() {
        this.state = co.edu.unillanos.elevator.domain.enums.DoorState.CLOSED;
    }

    public boolean isClosed() {
        return state == co.edu.unillanos.elevator.domain.enums.DoorState.CLOSED;
    }

    public boolean isOpen() {
        return state == co.edu.unillanos.elevator.domain.enums.DoorState.OPEN;
    }

    @Override
    public String toString() {
        return state.toString();
    }
}
