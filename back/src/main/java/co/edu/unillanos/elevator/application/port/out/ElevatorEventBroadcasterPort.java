package co.edu.unillanos.elevator.application.port.out;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventListener;

/**
 * Puerto de salida para la transmision de eventos de elevadores.
 * Define el contrato que implementara ElevatorEventBroadcaster.
 */
public interface ElevatorEventBroadcasterPort {
    
    /**
     * Suscribe un listener a los eventos del elevador
     */
    void subscribe(ElevatorEventListener listener);
    
    /**
     * Desuscribe un listener de los eventos
     */
    void unsubscribe(ElevatorEventListener listener);
    
    /**
     * Emite un evento a todos los listeners suscritos
     */
    void broadcast(ElevatorEventDTO event);
}
