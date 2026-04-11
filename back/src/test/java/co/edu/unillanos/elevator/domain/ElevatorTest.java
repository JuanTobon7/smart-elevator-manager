package co.edu.unillanos.elevator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import co.edu.unillanos.elevator.domain.enums.Direction;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;

/**
 * Tests unitarios para la lógica de dominio del Elevator (sin Spring)
 */
@DisplayName("Elevator Domain Tests")
class ElevatorTest {

    private Elevator elevator;

    @BeforeEach
    void setUp() {
        elevator = new Elevator();
    }

    @DisplayName("Estado inicial: piso 1, IDLE, puerta cerrada")
    @Test
    void testInitialState() {
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(ElevatorState.IDLE, elevator.getState());
        assertEquals(DoorState.CLOSED, elevator.getDoor().getState());
    }

    @DisplayName("Movimiento válido: IDLE → MOVING → IDLE")
    @Test
    void testValidMovement() {
        // Partir de IDLE
        assertEquals(ElevatorState.IDLE, elevator.getState());

        // Iniciar movimiento
        elevator.goToFloor(3);
        assertEquals(ElevatorState.MOVING, elevator.getState());
        assertEquals(3, elevator.getTargetFloor());
        assertEquals(Direction.UP, elevator.getDirection());

        // Llegar al destino
        elevator.arriveAtFloor();
        assertEquals(ElevatorState.IDLE, elevator.getState());
        assertEquals(3, elevator.getCurrentFloor());
        assertEquals(Direction.NONE, elevator.getDirection());
    }

    @DisplayName("goToFloor() con puerta abierta lanza excepción")
    @Test
    void testMoveWithOpenDoor() {
        // Abrir puerta
        elevator.openDoor();
        assertEquals(ElevatorState.DOOR_OPEN, elevator.getState());

        // Intentar movimiento
        ElevatorException ex = assertThrows(ElevatorException.class,
            () -> elevator.goToFloor(2));
        assertTrue(ex.getMessage().contains("puerta está abierta"));
    }

    @DisplayName("openDoor() en IDLE")
    @Test
    void testOpenDoor() {
        elevator.openDoor();
        assertEquals(ElevatorState.DOOR_OPEN, elevator.getState());
        assertEquals(DoorState.OPEN, elevator.getDoor().getState());
    }

    @DisplayName("closeDoor() transición DOOR_OPEN → DOOR_CLOSING")
    @Test
    void testCloseDoor() {
        elevator.openDoor();
        assertEquals(ElevatorState.DOOR_OPEN, elevator.getState());

        elevator.closeDoor();
        assertEquals(ElevatorState.DOOR_CLOSING, elevator.getState());
        assertEquals(DoorState.CLOSING, elevator.getDoor().getState());
    }

    @DisplayName("completeDoorClosing() transición DOOR_CLOSING → IDLE")
    @Test
    void testCompleteDoorClosing() {
        elevator.openDoor();
        elevator.closeDoor();

        elevator.completeDoorClosing();
        assertEquals(ElevatorState.IDLE, elevator.getState());
        assertEquals(DoorState.CLOSED, elevator.getDoor().getState());
    }

    @DisplayName("reset() desde ERROR → IDLE")
    @Test
    void testResetFromError() {
        elevator.setError("Test error");
        assertEquals(ElevatorState.ERROR, elevator.getState());

        elevator.reset();
        assertEquals(ElevatorState.IDLE, elevator.getState());
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(Direction.NONE, elevator.getDirection());
    }

    @DisplayName("goToFloor() con piso 0 lanza excepción")
    @Test
    void testInvalidFloorZero() {
        ElevatorException ex = assertThrows(ElevatorException.class,
            () -> elevator.goToFloor(0));
        assertTrue(ex.getMessage().contains("inválido"));
    }

    @DisplayName("goToFloor() con piso 6 lanza excepción")
    @Test
    void testInvalidFloorSix() {
        ElevatorException ex = assertThrows(ElevatorException.class,
            () -> elevator.goToFloor(6));
        assertTrue(ex.getMessage().contains("inválido"));
    }

    @DisplayName("readSensors() retorna SensorReading con estado actual")
    @Test
    void testReadSensors() {
        var reading = elevator.readSensors();
        assertEquals(1, reading.getFloor());
        assertEquals(DoorState.CLOSED, reading.getDoorState());
        assertEquals(ElevatorState.IDLE, reading.getElevatorState());
    }

    @DisplayName("goToFloor() del mismo piso no hace nada")
    @Test
    void testGoToCurrentFloor() {
        // Ya estamos en piso 1
        elevator.goToFloor(1);
        // No debe cambiar estado
        assertEquals(ElevatorState.IDLE, elevator.getState());
    }

    @DisplayName("Dirección DOWN cuando vamos a piso inferior")
    @Test
    void testDirectionDown() {
        // Primero ir a piso 5
        elevator.goToFloor(5);
        elevator.arriveAtFloor();

        // Luego bajar a piso 2
        elevator.goToFloor(2);
        assertEquals(Direction.DOWN, elevator.getDirection());
    }
}
