package co.edu.unillanos.elevator.application.port.in;

/**
 * Puerto de entrada para casos de uso relacionados con la puerta
 */
public interface DoorUseCase {
    
    /**
     * Abre la puerta del elevador
     */
    void open();

    /**
     * Cierra la puerta del elevador
     */
    void close();
}
