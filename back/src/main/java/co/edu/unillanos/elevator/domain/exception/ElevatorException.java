package co.edu.unillanos.elevator.domain.exception;

/**
 * Excepción indicando una operación inválida en el elevador
 */
public class ElevatorException extends RuntimeException {
    
    public ElevatorException(String message) {
        super(message);
    }

    public ElevatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
