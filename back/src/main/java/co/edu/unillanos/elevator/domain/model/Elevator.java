package co.edu.unillanos.elevator.domain.model;

import lombok.Getter;
import co.edu.unillanos.elevator.domain.enums.Direction;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;

/**
 * Entidad de dominio que representa el elevador.
 * Implementa máquina de estados y reglas de negocio sin depender de Spring.
 */
@Getter
public class Elevator {
    private static final int MIN_FLOOR = 1;
    private static final int MAX_FLOOR = 5;

    private int currentFloor;
    private int targetFloor;
    private ElevatorState state;
    private Direction direction;
    private Door door;

    public Elevator() {
        this.currentFloor = 1;
        this.targetFloor = 1;
        this.state = ElevatorState.IDLE;
        this.direction = Direction.NONE;
        this.door = new Door();
    }

    /**
     * Intenta mover el elevador a un piso especificado
     * Transición: IDLE → MOVING
     */
    public void goToFloor(int floor) {
        // Validar reglas de negocio
        if (!Floor.isValid(floor)) {
            throw new ElevatorException(
                String.format("Piso inválido: %d (rango válido: %d-%d)", floor, MIN_FLOOR, MAX_FLOOR)
            );
        }

        if (floor == currentFloor) {
            // Ya estamos en el piso
            return;
        }

        if (!door.isClosed()) {
            throw new ElevatorException("No se puede mover: la puerta está abierta");
        }

        if (state == ElevatorState.ERROR) {
            throw new ElevatorException("El elevador está en estado de error. Use reset() para recuperarse");
        }

        // Transición de estado
        this.targetFloor = floor;
        this.direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        this.state = ElevatorState.MOVING;
    }

    /**
     * Simula que el elevador ha llegado al piso destino
     * Transición: MOVING → IDLE
     */
    public void arriveAtFloor() {
        if (state != ElevatorState.MOVING) {
            throw new ElevatorException("El elevador no está en movimiento");
        }

        this.currentFloor = targetFloor;
        this.state = ElevatorState.IDLE;
        this.direction = Direction.NONE;
    }

    /**
     * Abre la puerta del elevador
     * Transición: IDLE → DOOR_OPEN
     */
    public void openDoor() {
        if (state != ElevatorState.IDLE) {
            throw new ElevatorException("Solo se puede abrir la puerta en estado IDLE");
        }

        door.open();
        this.state = ElevatorState.DOOR_OPEN;
    }

    /**
     * Inicia el cierre de la puerta
     * Transición: DOOR_OPEN → DOOR_CLOSING
     */
    public void closeDoor() {
        if (state != ElevatorState.DOOR_OPEN) {
            throw new ElevatorException("La puerta no está abierta");
        }

        door.startClosing();
        this.state = ElevatorState.DOOR_CLOSING;
    }

    /**
     * Completa el cierre de la puerta
     * Transición: DOOR_CLOSING → IDLE
     */
    public void completeDoorClosing() {
        if (state != ElevatorState.DOOR_CLOSING) {
            throw new ElevatorException("La puerta no está cerrándose");
        }

        door.close();
        this.state = ElevatorState.IDLE;
    }

    /**
     * Marca el elevador en estado de error
     */
    public void setError(String reason) {
        this.state = ElevatorState.ERROR;
        this.direction = Direction.NONE;
    }

    /**
     * Resetea el elevador a estado inicial
     */
    public void reset() {
        this.currentFloor = 1;
        this.targetFloor = 1;
        this.state = ElevatorState.IDLE;
        this.direction = Direction.NONE;
        this.door = new Door();
    }

    /**
     * Obtiene la lectura actual de sensores
     */
    public SensorReading readSensors() {
        return SensorReading.builder()
            .floor(currentFloor)
            .doorState(door.getState())
            .elevatorState(state)
            .build();
    }
}
