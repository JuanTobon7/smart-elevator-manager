package co.edu.unillanos.elevator.infrastructure.adapter;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.model.SensorReading;
import co.edu.unillanos.elevator.infrastructure.adapter.SerialPortManager.ArduinoException;
import co.edu.unillanos.elevator.infrastructure.adapter.SerialPortManager.ArduinoTimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Adaptador para comunicación con Arduino real
 * Activo solo con perfil "arduino"
 * 
 * Implementa el protocolo definido en ARDUINO_PROTOCOL.md
 * Comunicación serial a través de jSerialComm
 * 
 * @see SerialPortManager para detalles de comunicación
 */
@Component
@Profile("arduino")
public class ArduinoAdapter implements HardwarePort {

    private static final Logger log = LoggerFactory.getLogger(ArduinoAdapter.class);
    
    // Configuración del puerto serial
    @Value("${arduino.port:COM4}")
    private String arduinoPort;
    
    @Value("${arduino.enabled:true}")
    private boolean arduinoEnabled;
    
    private SerialPortManager serialPortManager;
    private volatile boolean initialized = false;
    
    public ArduinoAdapter() {
        this.serialPortManager = new SerialPortManager();
    }
    
    /**
     * Inicialización después de que Spring inyecte las propiedades
     */
    @PostConstruct
    public void init() {
        if (!arduinoEnabled) {
            log.warn("Arduino deshabilitado en configuración");
            return;
        }
        
        try {
            log.info("Inicializando conexión con Arduino en puerto: {}", arduinoPort);
            
            // Intentar conectar
            if (serialPortManager.connect(arduinoPort)) {
                
                // Verificar conexión con PING
                try {
                    Thread.sleep(2000);
                                
                    // Limpiar buffer: descartar "ARDUINO_ELEVATOR_READY" y cualquier otro residuo
                    flushInputBuffer();
                    String response = serialPortManager.sendCommand("PING");
                    if (response.equals("PONG")) {
                        initialized = true;
                        log.info("✓ Arduino conectado y respondiendo correctamente");
                        
                        // Resetear Arduino al estado inicial
                        reset();
                    } else {
                        log.error("Arduino respondió incorrectamente al PING: {}", response);
                    }
                } catch (ArduinoTimeoutException e) {
                    log.error("Timeout verificando conexión con Arduino", e);
                } catch (ArduinoException e) {
                    log.error("Error verificando conexión con Arduino", e);
                }
            } else {
                log.error("No se pudo conectar a Arduino en puerto: {}", arduinoPort);
                
                // Listar puertos disponibles
                String[] availablePorts = SerialPortManager.getAvailablePorts();
                if (availablePorts.length > 0) {
                    log.info("Puertos disponibles: {}", String.join(", ", availablePorts));
                }
            }
        } catch (Exception e) {
            log.error("Error fatal inicializando Arduino", e);
        }
    }
    
    /**
     * Cierre de recursos al apagar la aplicación
     */
    @PreDestroy
    public void shutdown() {
        if (serialPortManager != null) {
            serialPortManager.disconnect();
            log.info("Arduino desconectado");
        }
    }

    @Override
    public void executeCommand(String command) {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
        
        try {
            String response = serialPortManager.sendCommand(command);
            log.info("Comando ejecutado: {} → {}", command, response);
        } catch (ArduinoException e) {
            log.error("Error ejecutando comando en Arduino: {}", command, e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    @Override
    public SensorReading readState() throws InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
        
        try {
            // Enviar comando READ_STATE
            String response = serialPortManager.sendCommand("READ_STATE");
            
            // Parsear respuesta: STATE:FLOOR=2,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE
            return parseStateResponse(response);
            
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout leyendo estado de Arduino", e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error leyendo estado de Arduino", e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    @Override
    public void moveToFloor(int floor) {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
        
        // Validar piso
        if (floor < 1 || floor > 10) {
            log.error("Piso fuera de rango: {}", floor);
            throw new IllegalArgumentException("Piso debe estar entre 1 y 10");
        }
        
        try {
            // Enviar comando MOVE_TO_FLOOR
            String response = serialPortManager.sendCommand("MOVE_TO_FLOOR " + floor);
            
            if (response.contains("MOVING")) {
                log.info("Elevador moviéndose al piso {}", floor);
                
                // En producción, esperar a que Arduino confirme llegada
                // Esto dependerá del tiempo real que toma el movimiento
                // Por ahora, hacer polling del estado
                waitForFloor(floor);
            } else {
                log.warn("Respuesta inesperada al mover a piso {}: {}", floor, response);
            }
            
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout moviendo a piso {}", floor, e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error moviendo a piso {}", floor, e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    public void flushInputBuffer() {
        try {
            int available = serialPortManager.bytesAvailable();
            if (available > 0) {
                byte[] discard = new byte[available];
                serialPortManager.readBytes(discard, discard.length);
                log.debug("Buffer limpiado: {} bytes descartados", available);
            }
        } catch (Exception e) {
            log.warn("Error al limpiar buffer de entrada: {}", e.getMessage());
        }
    }

    @Override
    public void openDoor() {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
        
        try {
            Thread.sleep(2000);
            serialPortManager.flushInputBuffer(); // Limpiar buffer antes de enviar comando
            String response = serialPortManager.sendCommand("OPEN_DOOR");
            
            if (response.contains("OPENING")) {
                log.info("Puerta del elevador abriéndose");
                
                // Esperar a que se abra completamente (simulado: 1 segundo)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warn("Apertura de puerta interrumpida");
                    Thread.currentThread().interrupt();
                }
            } else {
                log.warn("Respuesta inesperada al abrir puerta: {}", response);
            }
            
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout abriendo puerta", e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error abriendo puerta", e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    @Override
    public void closeDoor() {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
        
        try {
            String response = serialPortManager.sendCommand("CLOSE_DOOR");
            
            if (response.contains("CLOSING")) {
                log.info("Puerta del elevador cerrándose");
                
                // Esperar a que se cierre completamente (simulado: 0.8 segundos)
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    log.warn("Cierre de puerta interrumpido");
                    Thread.currentThread().interrupt();
                }
            } else {
                log.warn("Respuesta inesperada al cerrar puerta: {}", response);
            }
            
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout cerrando puerta", e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error cerrando puerta", e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    @Override
    public void reset() {
        if (!initialized && !arduinoEnabled) {
            log.warn("Arduino no disponible, reset simulado");
            return;
        }
        
        try {
            String response = serialPortManager.sendCommand("RESET");
            log.info("Arduino reseteado: {}", response);
        } catch (ArduinoException e) {
            log.warn("Error reseteando Arduino", e);
            // No lanzar excepción, solo log de warning
        }
    }

    /**
     * Parsea respuesta del comando READ_STATE
     * Formato esperado: STATE:FLOOR=2,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE
     */
    private SensorReading parseStateResponse(String response) throws ArduinoException {
        try {
            if (!response.startsWith("STATE:")) {
                throw new ArduinoException("Formato de respuesta inválido: " + response);
            }
            
            String stateData = response.substring(6); // Remover "STATE:"
            String[] parts = stateData.split(",");
            
            if (parts.length < 3) {
                throw new ArduinoException("Respuesta incompleta: " + response);
            }
            
            // Parsear FLOOR=2
            int floor = Integer.parseInt(parts[0].split("=")[1]);
            
            // Parsear DOOR_STATE=CLOSED
            DoorState doorState = DoorState.valueOf(parts[1].split("=")[1]);
            
            // Parsear ELEVATOR_STATE=IDLE
            ElevatorState elevatorState = ElevatorState.valueOf(parts[2].split("=")[1]);
            
            return SensorReading.builder()
                .floor(floor)
                .doorState(doorState)
                .elevatorState(elevatorState)
                .build();
                
        } catch (Exception e) {
            throw new ArduinoException("Error parseando respuesta: " + response, e);
        }
    }
    
    /**
     * Espera a que el elevador llegue al piso especificado
     * Usa polling del estado
     */
    private void waitForFloor(int targetFloor) {
        int maxRetries = 60; // 60 segundos max (1 segundo por intento)
        int retry = 0;
        
        while (retry < maxRetries) {
            try {
                try {
                    SensorReading reading = readState();
                    
                    if (reading.getFloor() == targetFloor && 
                        reading.getElevatorState() == ElevatorState.IDLE) {
                        log.info("Elevador llegó al piso {}", targetFloor);
                        return;
                    }
                } catch (InterruptedException e) {
                    log.warn("Polling interrumpido");
                    Thread.currentThread().interrupt();
                    return;
                }
                
                try {
                    Thread.sleep(1000); // Esperar 1 segundo antes del siguiente polling
                } catch (InterruptedException e) {
                    log.warn("Polling interrumpido");
                    Thread.currentThread().interrupt();
                    return;
                }
                retry++;
                
            } catch (RuntimeException e) {
                log.warn("Error durante polling de estado: {}", e.getMessage());
                retry++;
            }
        }
        
        log.warn("Timeout esperando llegada al piso {}", targetFloor);
    }
}
