package co.edu.unillanos.elevator.infrastructure.event;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;

/**
 * Interfaz para listeners de eventos de elevadores
 * Los clientes websocket/SSE implementan esta interfaz
 */
public interface ElevatorEventListener {
    /**
     * Se invoca cuando ocurre un evento
     */
    void onEvent(ElevatorEventDTO event) throws Exception;
    
    /**
     * Verifica si este listener está suscrito a los eventos de un elevador específico
     */
    boolean isSubscribedTo(String elevatorId);
}
