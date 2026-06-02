package co.edu.unillanos.elevator.domain.model;

import lombok.Getter;
import co.edu.unillanos.elevator.domain.enums.Direction;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;

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
        this.targetFloor  = 1;
        this.state        = ElevatorState.IDLE;
        this.direction    = Direction.NONE;
        this.door         = new Door();
    }

    /**
     * Intenta mover el elevador a un piso especificado.
     * Transición: IDLE → GOING_UP | GOING_DOWN
     */
    public void goToFloor(int floor) {
        if (!Floor.isValid(floor)) {
            throw new ElevatorException(
                String.format("Piso inválido: %d (rango válido: %d-%d)",
                              floor, MIN_FLOOR, MAX_FLOOR)
            );
        }

        if (floor == currentFloor) {
            return;
        }

        if (!door.isClosed()) {
            throw new ElevatorException("No se puede mover: la puerta está abierta");
        }

        if (state == ElevatorState.ERROR) {
            throw new ElevatorException(
                "El elevador está en estado de error. Use reset() para recuperarse"
            );
        }

        if (state == ElevatorState.EMERGENCY_STOP) {
            throw new ElevatorException(
                "El elevador tiene una parada de emergencia activa. Use reset() para recuperarse"
            );
        }

        this.targetFloor = floor;

        // ← MODIFICADO: estados direccionales en lugar de MOVING genérico
        if (floor > currentFloor) {
            this.direction = Direction.UP;
            this.state     = ElevatorState.GOING_UP;
        } else {
            this.direction = Direction.DOWN;
            this.state     = ElevatorState.GOING_DOWN;
        }
    }

    /**
     * Registra el progreso durante el desplazamiento.
     * Llamar cada vez que el Arduino reporta MOVING:NOW_AT_FLOOR_N.
     * No cambia el estado — el elevador sigue en GOING_UP / GOING_DOWN.
     */
    public void updateFloorInTransit(int floor) {
        if (state != ElevatorState.GOING_UP && state != ElevatorState.GOING_DOWN) {
            throw new ElevatorException("El elevador no está en movimiento");
        }

        if (!Floor.isValid(floor)) {
            throw new ElevatorException("Piso en tránsito inválido: " + floor);
        }

        this.currentFloor = floor;
        // dirección se mantiene; state no cambia
    }

    /**
     * El elevador ha llegado al piso destino.
     * Transición: GOING_UP | GOING_DOWN → IDLE
     */
    public void arriveAtFloor() {
        if (state != ElevatorState.GOING_UP && state != ElevatorState.GOING_DOWN) {
            throw new ElevatorException("El elevador no está en movimiento");
        }

        this.currentFloor = targetFloor;
        this.state        = ElevatorState.IDLE;
        this.direction    = Direction.NONE;
    }

    /**
     * Abre la puerta del elevador.
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
     * Inicia el cierre de la puerta.
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
     * Completa el cierre de la puerta.
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
     * Activa la parada de emergencia desde cualquier estado de movimiento.
     * Transición: GOING_UP | GOING_DOWN → EMERGENCY_STOP
     */
    public void emergencyStop() {
        if (state != ElevatorState.GOING_UP && state != ElevatorState.GOING_DOWN) {
            throw new ElevatorException(
                "La parada de emergencia solo aplica durante el movimiento " +
                "(estado actual: " + state + ")"
            );
        }

        this.state     = ElevatorState.EMERGENCY_STOP;
        this.direction = Direction.NONE;
        // currentFloor queda en la última posición conocida reportada por el sensor
    }

    /**
     * Recupera el elevador desde EMERGENCY_STOP hacia IDLE.
     * Transición: EMERGENCY_STOP → IDLE
     * El piso de recuperación lo confirma el hardware mediante READ_STATE.
     */
    public void recoverFromEmergency(int confirmedFloor) {
        if (state != ElevatorState.EMERGENCY_STOP) {
            throw new ElevatorException(
                "No hay parada de emergencia activa (estado actual: " + state + ")"
            );
        }

        if (!Floor.isValid(confirmedFloor)) {
            throw new ElevatorException("Piso de recuperación inválido: " + confirmedFloor);
        }

        this.currentFloor = confirmedFloor;
        this.targetFloor  = confirmedFloor;
        this.state        = ElevatorState.IDLE;
        this.direction    = Direction.NONE;
    }

    /**
     * Marca el elevador en estado de error genérico (fallo de hardware, etc.).
     */
    public void setError(String reason) {
        this.state     = ElevatorState.ERROR;
        this.direction = Direction.NONE;
    }

    /**
     * Resetea completamente el elevador a estado inicial.
     * Válido desde cualquier estado, incluido ERROR y EMERGENCY_STOP.
     */
    public void reset() {
        this.currentFloor = 1;
        this.targetFloor  = 1;
        this.state        = ElevatorState.IDLE;
        this.direction    = Direction.NONE;
        this.door         = new Door();
    }

    /**
     * Indica si el elevador está en tránsito (subiendo o bajando).
     */
    public boolean isMoving() {
        return state == ElevatorState.GOING_UP || state == ElevatorState.GOING_DOWN;
    }

    /**
     * Obtiene la lectura actual de sensores.
     */
    public SensorReading readSensors() {
        return SensorReading.builder()
            .floor(currentFloor)
            .doorState(door.getState())
            .elevatorState(state)
            .build();
    }
    /**
     * Sincroniza el piso actual desde hardware sin cambiar otros estados.
     * Solo para uso del orquestador — no es una transición de negocio.
     */
    public void syncFloor(int floor) {
        this.currentFloor = floor;
    }

    /**
     * Fuerza estado DOOR_OPEN cuando el hardware lo confirma
     * pero el dominio quedó desincronizado en IDLE.
     */
    public void forceDoorOpen() {
        door.open();
        this.state = ElevatorState.DOOR_OPEN;
    }

    /**
     * Fuerza estado IDLE + puerta cerrada cuando el hardware lo confirma
     * pero el dominio quedó desincronizado en DOOR_OPEN o DOOR_CLOSING.
     */
    public void forceDoorClosed() {
        door.close();
        this.state = ElevatorState.IDLE;
    }
}