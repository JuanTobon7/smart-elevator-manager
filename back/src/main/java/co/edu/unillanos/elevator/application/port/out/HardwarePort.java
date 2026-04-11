package co.edu.unillanos.elevator.application.port.out;

import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Puerto de salida que define la interfaz con el hardware (elevador real o simulado)
 */
public interface HardwarePort {
    
    /**
     * Ejecuta un comando en el hardware del elevador
     */
    void executeCommand(String command);

    /**
     * Lee el estado actual del hardware
     */
    SensorReading readState() throws InterruptedException;

    /**
     * Inicia un movimiento del elevador al piso especificado
     */
    void moveToFloor(int floor);

    /**
     * Abre la puerta del elevador
     */
    void openDoor();

    /**
     * Cierra la puerta del elevador
     */
    void closeDoor();

    /**
     * Resetea el hardware al estado inicial
     */
    void reset();
}
