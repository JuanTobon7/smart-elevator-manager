package co.edu.unillanos.elevator.infrastructure.event;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorEventDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Implementación de ElevatorEventListener que usa SSE
 */
public class SseElevatorEventListener implements ElevatorEventListener {
    private static final Logger log = LoggerFactory.getLogger(SseElevatorEventListener.class);
    
    private final SseEmitter emitter;
    private final String elevatorId;
    private volatile boolean isActive = true;
    
    public SseElevatorEventListener(SseEmitter emitter, String elevatorId) {
        this.emitter = emitter;
        this.elevatorId = elevatorId;
    }
    
    @Override
    public void onEvent(ElevatorEventDTO event) throws Exception {
        if (!isActive) {
            throw new IllegalStateException("Listener is inactive");
        }
        
        try {
            SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
                    .id(System.currentTimeMillis() + "")
                    .name(event.getEventType())
                    .data(event)
                    .reconnectTime(5000);
            
            emitter.send(sseEvent);
            log.debug("Evento enviado a cliente SSE para elevador: {}", elevatorId);
        } catch (ClientAbortException e) {
            log.debug("Cliente SSE desconectado para elevador {}", elevatorId);
            isActive = false;
            throw e;
        } catch (IOException e) {
            log.debug("Error de conexión SSE para elevador {}: {}", elevatorId, e.getMessage());
            isActive = false;
            throw e;
        }
    }
    
    @Override
    public boolean isSubscribedTo(String elevatorId) {
        return isActive && (this.elevatorId.equals(elevatorId) || "all".equals(this.elevatorId));
    }
    
    public boolean isActive() {
        return isActive;
    }
}
