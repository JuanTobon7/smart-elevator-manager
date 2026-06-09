package co.edu.unillanos.elevator.domain.enums;

/**
 * Estados posibles del elevador
 */
public enum ElevatorState {
    IDLE,
    GOING_UP,       
    GOING_DOWN,     
    DOOR_OPEN,
    DOOR_OPENING,
    DOOR_CLOSING,
    EMERGENCY_STOP,
    ERROR  
    
}