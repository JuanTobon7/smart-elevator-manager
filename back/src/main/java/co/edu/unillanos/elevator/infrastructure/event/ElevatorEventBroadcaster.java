package co.edu.unillanos.elevator.infrastructure.event;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.IOException;

/**
 * Broadcaster de eventos de elevadores usando observador pattern
 * Permite que múltiples clientes se suscriban a eventos en tiempo real via SSE
 */
@Component
public class ElevatorEventBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(ElevatorEventBroadcaster.class);
    
    private final List<ElevatorEventListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * Suscribe un listener a los eventos del elevador
     */
    public void subscribe(ElevatorEventListener listener) {
        listeners.add(listener);
        log.debug("Listener suscrito. Total de listeners: {}", listeners.size());
    }
    
    /**
     * Desuscribe un listener de los eventos
     */
    public void unsubscribe(ElevatorEventListener listener) {
        listeners.remove(listener);
        log.debug("Listener desuscrito. Total de listeners: {}", listeners.size());
    }
    
    /**
     * Emite un evento a todos los listeners suscritos
     */
    public void broadcast(ElevatorEventDTO event) {
        log.debug("Broadcasting event for elevatorId: {} | Event: {}", 
                 event.getElevatorId(), event.getEventType());
        
        List<ElevatorEventListener> listenersToRemove = new ArrayList<>();
        
        for (ElevatorEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (org.apache.catalina.connector.ClientAbortException e) {
                log.debug("Cliente SSE desconectado - removiendo listener");
                listenersToRemove.add(listener);
            } catch (java.io.IOException e) {
                log.debug("Error de conexión SSE - removiendo listener: {}", e.getMessage());
                listenersToRemove.add(listener);
            } catch (Exception e) {
                log.warn("Error notificando listener: {}", e.getMessage());
                listenersToRemove.add(listener);
            }
        }
        
        // Remover listeners inactivos
        for (ElevatorEventListener listener : listenersToRemove) {
            listeners.remove(listener);
            log.debug("Listener removido. Total de listeners: {}", listeners.size());
        }
    }
    
    /**
     * Emite un evento a un elevador específico
     */
    public void broadcastToElevator(String elevatorId, ElevatorEventDTO event) {
        log.debug("Broadcasting event to elevatorId: {} | Event: {}", 
                 elevatorId, event.getEventType());
        
        List<ElevatorEventListener> listenersToRemove = new ArrayList<>();
        
        for (ElevatorEventListener listener : listeners) {
            if (listener.isSubscribedTo(elevatorId)) {
                try {
                    listener.onEvent(event);
                } catch (org.apache.catalina.connector.ClientAbortException e) {
                    log.debug("Cliente SSE desconectado - removiendo listener");
                    listenersToRemove.add(listener);
                } catch (java.io.IOException e) {
                    log.debug("Error de conexión SSE - removiendo listener: {}", e.getMessage());
                    listenersToRemove.add(listener);
                } catch (Exception e) {
                    log.warn("Error notificando listener: {}", e.getMessage());
                    listenersToRemove.add(listener);
                }
            }
        }
        
        // Remover listeners inactivos
        for (ElevatorEventListener listener : listenersToRemove) {
            listeners.remove(listener);
            log.debug("Listener removido. Total de listeners: {}", listeners.size());
        }
    }
    
    /**
     * Obtiene el número de listeners activos
     */
    public int getActiveListenersCount() {
        return listeners.size();
    }
}
