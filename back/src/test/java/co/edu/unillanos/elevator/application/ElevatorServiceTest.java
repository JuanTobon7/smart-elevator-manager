package co.edu.unillanos.elevator.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

import co.edu.unillanos.elevator.application.port.in.DoorUseCase;
import co.edu.unillanos.elevator.application.port.in.ElevatorUseCase;
import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Tests de integración para los servicios del elevador con SimulatorAdapter
 */
@SpringBootTest
@ActiveProfiles("simulator")
@DisplayName("Elevator Service Integration Tests")
class ElevatorServiceTest {

    @Autowired
    private ElevatorUseCase elevatorUseCase;

    @Autowired
    private DoorUseCase doorUseCase;

    @BeforeEach
    void setUp() {
        // Resetear elevador antes de cada test
        elevatorUseCase.reset();
    }

    @DisplayName("Comando GO 3 ejecuta correctamente")
    @Test
    void testGoToFloor() throws InterruptedException {
        elevatorUseCase.goToFloor(3);
        SensorReading reading = elevatorUseCase.readSensors();
        assertEquals(3, reading.getFloor());
    }

    @DisplayName("Secuencia: GO 2 → READ → DOOR OPEN → DOOR CLOSE → GO 4")
    @Test
    void testCompleteSequence() throws InterruptedException {
        // GO 2
        elevatorUseCase.goToFloor(2);
        SensorReading reading = elevatorUseCase.readSensors();
        assertEquals(2, reading.getFloor());

        // DOOR OPEN
        doorUseCase.open();
        reading = elevatorUseCase.readSensors();
        assertEquals("OPEN", reading.getDoorState().toString());

        // DOOR CLOSE
        doorUseCase.close();
        reading = elevatorUseCase.readSensors();
        assertEquals("CLOSED", reading.getDoorState().toString());

        // GO 4
        elevatorUseCase.goToFloor(4);
        reading = elevatorUseCase.readSensors();
        assertEquals(4, reading.getFloor());
    }

    @DisplayName("RESET devuelve floor=1, state=IDLE, door=CLOSED")
    @Test
    void testReset() {
        elevatorUseCase.reset();
        SensorReading reading = elevatorUseCase.readSensors();
        
        assertEquals(1, reading.getFloor());
        assertEquals("IDLE", reading.getElevatorState().toString());
        assertEquals("CLOSED", reading.getDoorState().toString());
    }

    @DisplayName("Movimiento desde piso inicial a piso 5")
    @Test
    void testMovementToFloor5() {
        elevatorUseCase.goToFloor(5);
        SensorReading reading = elevatorUseCase.readSensors();
        assertEquals(5, reading.getFloor());
    }

    @DisplayName("Puerta se puede abrir y cerrar múltiples veces")
    @Test
    void testMultipleDoorSequences() {
        for (int i = 0; i < 3; i++) {
            doorUseCase.open();
            SensorReading reading = elevatorUseCase.readSensors();
            assertEquals("OPEN", reading.getDoorState().toString());

            doorUseCase.close();
            reading = elevatorUseCase.readSensors();
            assertEquals("CLOSED", reading.getDoorState().toString());
        }
    }
}
