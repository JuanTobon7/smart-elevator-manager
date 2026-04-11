package co.edu.unillanos.elevator.application.port.in;

import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Puerto de entrada para casos de uso relacionados con el movimiento del elevador
 */
public interface ElevatorUseCase {
    
    /**
     * Mueve el elevador a un piso especificado
     */
    void goToFloor(int floor);

    /**
     * Retorna la lectura actual de sensores del elevador
     */
    SensorReading readSensors();

    /**
     * Resetea el elevador a su estado inicial
     */
    void reset();
}
