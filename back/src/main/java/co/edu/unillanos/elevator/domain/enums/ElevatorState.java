package co.edu.unillanos.elevator.domain.enums;

/**
 * Estados posibles del elevador
 */
public enum ElevatorState {
    IDLE,           // Elevador parado, listo para recibir comandos
    MOVING,         // Elevador en movimiento
    DOOR_OPEN,      // Puerta abierta
    DOOR_CLOSING,   // Puerta cerrándose
    ERROR           // Estado de error
}
