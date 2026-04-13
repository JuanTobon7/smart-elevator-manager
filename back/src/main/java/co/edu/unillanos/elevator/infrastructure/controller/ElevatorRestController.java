package co.edu.unillanos.elevator.infrastructure.controller;

import co.edu.unillanos.elevator.infrastructure.dto.ElevatorStateDTO;
import co.edu.unillanos.elevator.infrastructure.dto.GenericResponseDTO;
import co.edu.unillanos.elevator.infrastructure.dto.RequestFloorDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.unillanos.elevator.infrastructure.elevator.ElevatorManager;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import co.edu.unillanos.elevator.infrastructure.event.SseElevatorEventListener;
import co.edu.unillanos.elevator.domain.exception.ElevatorException;
import co.edu.unillanos.elevator.domain.model.Elevator;

import java.io.IOException;
import java.util.Map;

/**
 * REST Controller para el API de elevadores
 * Proporciona endpoints para:
 * - Obtener estado de elevadores
 * - Solicitar movimiento de elevador
 * - Controlar puertas
 * - Suscribirse a eventos SSE
 */
@RestController
@RequestMapping("/api/elevators")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ElevatorRestController {
    private static final Logger log = LoggerFactory.getLogger(ElevatorRestController.class);
    
    private final ElevatorManager elevatorManager;
    private final ElevatorEventBroadcaster eventBroadcaster;
    
    @Autowired
    public ElevatorRestController(ElevatorManager elevatorManager, ElevatorEventBroadcaster eventBroadcaster) {
        this.elevatorManager = elevatorManager;
        this.eventBroadcaster = eventBroadcaster;
    }
    
    /**
     * GET /api/elevators
     * Obtiene el estado de todos los elevadores
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDTO<Map<String, ElevatorStateDTO>>> getAllElevators() {
        try {
            log.info("Obteniendo estado de todos los elevadores");
            
            // Si no hay elevadores, crear 3 por defecto
            if (elevatorManager.getAllElevators().isEmpty()) {
                for (int i = 1; i <= 3; i++) {
                    elevatorManager.getOrCreateElevator("elev-" + i);
                }
            }
            
            Map<String, ElevatorStateDTO> states = elevatorManager.getAllElevatorStates();
            return ResponseEntity.ok(GenericResponseDTO.ok(states, "Elevadores obtenidos exitosamente"));
        } catch (Exception e) {
            log.error("Error obteniendo elevadores: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenericResponseDTO.error("Error obteniendo elevadores: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/elevators/{id}
     * Obtiene el estado de un elevador específico
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDTO<ElevatorStateDTO>> getElevator(@PathVariable String id) {
        try {
            log.info("Obteniendo elevador: {}", id);
            
            Elevator elevator = elevatorManager.getOrCreateElevator(id);
            ElevatorStateDTO state = elevatorManager.getElevatorState(id);
            
            return ResponseEntity.ok(GenericResponseDTO.ok(state, "Elevador obtenido exitosamente"));
        } catch (Exception e) {
            log.error("Error obteniendo elevador {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenericResponseDTO.error("Error obteniendo elevador: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/elevators/{id}/request-floor
     * Solicita que el elevador se mueva a un piso específico
     */
    @PostMapping(value = "/{id}/request-floor", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDTO<ElevatorStateDTO>> requestFloor(
            @PathVariable String id,
            @RequestBody RequestFloorDTO request) {
        try {
            log.info("Solicitando movimiento del elevador {} al piso: {}", id, request.getFloor());
            
            var asyncService = elevatorManager.getAsyncService(id);
            
            // Ejecutar el movimiento de forma asincrónica
            asyncService.goToFloorAsync(request.getFloor())
                    .thenAccept(state -> log.info("Elevador {} llegó al piso {}", id, request.getFloor()))
                    .exceptionally(ex -> {
                        log.error("Error en movimiento de elevador {}: {}", id, ex.getMessage());
                        return null;
                    });
            
            ElevatorStateDTO state = elevatorManager.getElevatorState(id);
            return ResponseEntity.accepted().body(
                    GenericResponseDTO.ok(state, "Solicitud de movimiento aceptada")
            );
        } catch (ElevatorException e) {
            log.error("Error de elevador {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(GenericResponseDTO.error("Error de elevador: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error procesando solicitud para elevador {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenericResponseDTO.error("Error procesando solicitud: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/elevators/{id}/open-door
     * Abre la puerta del elevador
     */
    @PostMapping(value = "/{id}/open-door", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDTO<ElevatorStateDTO>> openDoor(@PathVariable String id) {
        try {
            log.info("Abriendo puerta del elevador: {}", id);
            
            var asyncService = elevatorManager.getAsyncService(id);
            
            asyncService.openDoorAsync()
                    .exceptionally(ex -> {
                        log.error("Error abriendo puerta de {}: {}", id, ex.getMessage());
                        return null;
                    });
            
            ElevatorStateDTO state = elevatorManager.getElevatorState(id);
            return ResponseEntity.accepted().body(
                    GenericResponseDTO.ok(state, "Solicitud para abrir puerta aceptada")
            );
        } catch (ElevatorException e) {
            log.error("Error de elevador {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(GenericResponseDTO.error("Error de elevador: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error abriendo puerta de {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenericResponseDTO.error("Error abriendo puerta: " + e.getMessage()));
        }
    }
    
    /**
     * POST /api/elevators/{id}/close-door
     * Cierra la puerta del elevador
     */
    @PostMapping(value = "/{id}/close-door", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDTO<ElevatorStateDTO>> closeDoor(@PathVariable String id) {
        try {
            log.info("Cerrando puerta del elevador: {}", id);
            
            var asyncService = elevatorManager.getAsyncService(id);
            
            asyncService.closeDoorAsync()
                    .exceptionally(ex -> {
                        log.error("Error cerrando puerta de {}: {}", id, ex.getMessage());
                        return null;
                    });
            
            ElevatorStateDTO state = elevatorManager.getElevatorState(id);
            return ResponseEntity.accepted().body(
                    GenericResponseDTO.ok(state, "Solicitud para cerrar puerta aceptada")
            );
        } catch (ElevatorException e) {
            log.error("Error de elevador {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(GenericResponseDTO.error("Error de elevador: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error cerrando puerta de {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(GenericResponseDTO.error("Error cerrando puerta: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/elevators/{id}/subscribe
     * Abre una conexión SSE para recibir eventos en tiempo real del elevador
     */
    @GetMapping(value = "/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToElevator(@PathVariable String id) {
        log.info("Cliente suscrito a eventos del elevador: {}", id);

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Verificar que el elevador existe ANTES de suscribir
        ElevatorStateDTO initialState;
        try {
            initialState = elevatorManager.getElevatorState(id);
        } catch (ElevatorException e) {
            log.error("Elevador no encontrado al suscribir: {}", id);
            emitter.completeWithError(e); // rechaza la conexión limpiamente
            return emitter;
        }

        SseElevatorEventListener listener = new SseElevatorEventListener(emitter, id);
        eventBroadcaster.subscribe(listener);

        // Ahora sí enviar estado inicial (elevador garantizado que existe)
        try {
            emitter.send(SseEmitter.event()
                    .id(System.currentTimeMillis() + "")
                    .name("CONNECTED")
                    .data(initialState)
                    .reconnectTime(5000));
        } catch (Exception e) {
            log.error("Error enviando evento inicial para elevador {}: {}", id, e.getMessage());
        }

        emitter.onCompletion(() -> eventBroadcaster.unsubscribe(listener));
        emitter.onTimeout(() -> eventBroadcaster.unsubscribe(listener));
        emitter.onError(throwable -> {
            if (throwable instanceof ClientAbortException ||
                (throwable.getCause() instanceof IOException && 
                throwable.getCause().getMessage() != null &&
                throwable.getCause().getMessage().contains("anulado"))) {
                log.debug("Cliente SSE desconectado para elevador: {}", id);
            } else {
                log.error("Error en conexión SSE para elevador {}: {}", id, throwable.getMessage());
            }
            eventBroadcaster.unsubscribe(listener);
        });

        return emitter;
    }
}
