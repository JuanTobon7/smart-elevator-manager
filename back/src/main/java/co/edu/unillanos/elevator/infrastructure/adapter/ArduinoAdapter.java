package co.edu.unillanos.elevator.infrastructure.adapter;

import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.enums.DoorState;
import co.edu.unillanos.elevator.domain.enums.ElevatorState;
import co.edu.unillanos.elevator.domain.model.SensorReading;
import co.edu.unillanos.elevator.infrastructure.adapter.SerialPortManager.ArduinoException;
import co.edu.unillanos.elevator.infrastructure.adapter.SerialPortManager.ArduinoTimeoutException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("arduino")
public class ArduinoAdapter implements HardwarePort {

    private static final Logger log = LoggerFactory.getLogger(ArduinoAdapter.class);

    @Value("${arduino.port:COM4}")
    private String arduinoPort;

    @Value("${arduino.enabled:true}")
    private boolean arduinoEnabled;

    private SerialPortManager serialPortManager;
    private volatile boolean initialized = false;

    public ArduinoAdapter() {
        this.serialPortManager = new SerialPortManager();
    }

    @PostConstruct
    public void init() {
        if (!arduinoEnabled) {
            log.warn("Arduino deshabilitado en configuración");
            return;
        }

        try {
            log.info("Inicializando conexión con Arduino en puerto: {}", arduinoPort);

            if (serialPortManager.connect(arduinoPort)) {
                try {
                    Thread.sleep(2000);
                    flushInputBuffer();
                    String response = serialPortManager.sendCommand("PING");
                    if ("PONG".equals(response)) {
                        initialized = true;
                        log.info("Arduino conectado y respondiendo correctamente");
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
                String[] availablePorts = SerialPortManager.getAvailablePorts();
                if (availablePorts.length > 0) {
                    log.info("Puertos disponibles: {}", String.join(", ", availablePorts));
                }
            }
        } catch (Exception e) {
            log.error("Error fatal inicializando Arduino", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (serialPortManager != null) {
            serialPortManager.disconnect();
            log.info("Arduino desconectado");
        }
    }

    public boolean isAvailable() {
        return initialized && serialPortManager != null && serialPortManager.isConnected();
    }

    @Override
    public void executeCommand(String command) {
        ensureInitialized();

        try {
            String response = serialPortManager.sendCommand(command);
            log.info("Comando ejecutado: {} -> {}", command, response);
        } catch (ArduinoException e) {
            log.error("Error ejecutando comando en Arduino: {}", command, e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }

    
    @Override
    public SensorReading readState() throws InterruptedException {
        ensureInitialized();
        int attempts = 0;
        
        while (attempts < 5) {
            try {
                String response = serialPortManager.sendCommand("READ_STATE");
                
                if (response == null || response.trim().isEmpty()) {
                    attempts++;
                    continue;
                }
                
                if (response.startsWith("STATUS:") || response.startsWith("MOVING:") 
                    || response.startsWith("OK:ARRIVED") || response.startsWith("ERROR:007")) {
                    Thread.sleep(150); // Damos un respiro al Arduino
                    attempts++;
                    continue; 
                }
                
                return parseStateResponse(response);
                
            } catch (ArduinoTimeoutException e) {
                log.error("Timeout leyendo estado de Arduino", e);
                throw new RuntimeException("Timeout: " + e.getMessage(), e);
            } catch (ArduinoException e) {
                log.warn("Fragmento serial corrupto, limpiando y reintentando... ({})", e.getMessage());
                attempts++;
                Thread.sleep(200);
            }
        }
        
        throw new RuntimeException("No se pudo obtener un estado limpio de hardware después de 5 intentos");
    }

    @Override
    public void moveToFloor(int floor) {
        ensureInitialized();

        if (floor < 1 || floor > 10) {
            throw new IllegalArgumentException("Piso debe estar entre 1 y 10");
        }

        try {
            SensorReading currentState = readState();
            boolean puertaCerrada;
            if (currentState.getSensorPuertaCerrada() != null) {
                puertaCerrada = currentState.getSensorPuertaCerrada();
            } else {
                puertaCerrada = currentState.getDoorState() == DoorState.CLOSED;
                log.warn("SENSOR_PUERTA no disponible, usando DOOR_STATE como fallback");
            }
            if (!puertaCerrada) {
                throw new IllegalStateException(
                    "Movimiento bloqueado: sensor físico detecta puerta abierta"
                );
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo verificar estado de puerta: {}", e.getMessage());
        }

        try {
            log.info("Solicitando movimiento a piso {} al Arduino", floor);

           
            flushInputBuffer();

            String response = serialPortManager.sendCommand("MOVE_TO_FLOOR " + floor);
            log.info("Respuesta inicial a MOVE_TO_FLOOR {}: {}", floor, response);

            
            if (response.contains("ALREADY_AT") || response.contains("OK:ALREADY_AT")) {
                log.info("Elevador ya estaba en piso {}", floor);
                return; // único caso donde no hace falta polling
            }

           
            waitForFloor(floor);

        } catch (ArduinoTimeoutException e) {
            log.error("Timeout moviendo a piso {}", floor, e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            if (e.getMessage().contains("004") || e.getMessage().contains("Door not closed")) {
                log.warn("Arduino rechazó movimiento por puerta: {}", e.getMessage());
                throw new RuntimeException("Puerta no confirmada por Arduino: " + e.getMessage(), e);
            }
            log.error("Error moviendo a piso {}", floor, e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void emergencyStop() {
        ensureInitialized();

        try {
            log.warn("Enviando EMERGENCY_STOP al Arduino");
            String response = serialPortManager.sendCommand("EMERGENCY_STOP");

            if (response.contains("OK:EMERGENCY_STOP")) {
                log.warn("Arduino confirmó parada de emergencia");
                waitForEmergencyStop();
            } else {
                log.error("Respuesta inesperada a EMERGENCY_STOP: {}", response);
            }
        } catch (ArduinoTimeoutException e) {
            
            log.warn("Timeout en respuesta a EMERGENCY_STOP (puede ser normal): {}", e.getMessage());
        } catch (ArduinoException e) {
            log.error("Error enviando EMERGENCY_STOP", e);
            throw new RuntimeException("Error de Arduino en emergencia: " + e.getMessage(), e);
        }
    }

    
    private void waitForEmergencyStop() {
        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {
            try {
                SensorReading reading = readState();

                if (reading.getElevatorState() == ElevatorState.EMERGENCY_STOP) {
                    log.warn("Estado EMERGENCY_STOP confirmado por sensor en piso {}",
                             reading.getFloor());
                    return;
                }

                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.warn("Error durante polling de emergencia: {}", e.getMessage());
            }
        }

        log.warn("Timeout esperando confirmación de EMERGENCY_STOP");
    }

    public void flushInputBuffer() {
        try {
            serialPortManager.flushInputBuffer();
        } catch (Exception e) {
            log.warn("Error al limpiar buffer de entrada: {}", e.getMessage());
        }
    }

    @Override
    public void openDoor() {
        ensureInitialized();

        try {
            Thread.sleep(2000);
            flushInputBuffer();
            String response = serialPortManager.sendCommand("OPEN_DOOR");

            if (response.contains("OPENING")) {
                log.info("Puerta abriéndose, esperando confirmación del sensor...");
                waitForDoorState(DoorState.OPEN, 7000);
            } else if (response.contains("ERROR:006")) {
                log.error("Timeout en Arduino abriendo puerta");
                throw new RuntimeException("Arduino: timeout abriendo puerta");
            } else {
                log.warn("Respuesta inesperada al abrir puerta: {}", response);
            }
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout abriendo puerta", e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error abriendo puerta", e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida al abrir puerta", e);
        }
    }

    @Override
    public void closeDoor() {
        ensureInitialized();

        try {
            String response = serialPortManager.sendCommand("CLOSE_DOOR");

            if (response.contains("CLOSING")) {
                log.info("Puerta cerrándose, esperando confirmación del sensor...");
                waitForDoorState(DoorState.CLOSED, 7000);
            } else if (response.contains("ERROR:006")) {
                log.error("Timeout en Arduino cerrando puerta");
                throw new RuntimeException("Arduino: timeout cerrando puerta");
            } else {
                log.warn("Respuesta inesperada al cerrar puerta: {}", response);
            }
        } catch (ArduinoTimeoutException e) {
            log.error("Timeout cerrando puerta", e);
            throw new RuntimeException("Timeout: " + e.getMessage(), e);
        } catch (ArduinoException e) {
            log.error("Error cerrando puerta", e);
            throw new RuntimeException("Error de Arduino: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida al cerrar puerta", e);
        }
    }

    private void waitForDoorState(DoorState expected, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                SensorReading reading = readState();

                // Si el sensor no llegó, confiar en el doorState lógico
                Boolean puertaCerrada = reading.getSensorPuertaCerrada();

                boolean sensorConfirma;
                if (puertaCerrada != null) {
                    sensorConfirma = (expected == DoorState.CLOSED)
                            ? puertaCerrada
                            : !puertaCerrada;
                } else {
                    sensorConfirma = reading.getDoorState() == expected;
                    log.warn("SENSOR_PUERTA no disponible en polling, usando DoorState");
                }

                if (sensorConfirma) {
                    log.info("Puerta confirmada en estado: {}", expected);
                    return;
                }

                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.warn("Error durante polling de puerta: {}", e.getMessage());
            }
        }

        log.warn("Timeout esperando puerta en estado: {}", expected);
    }

    @Override
    public void reset() {
        if (!isAvailable() && !arduinoEnabled) {
            log.warn("Arduino no disponible, reset simulado");
            return;
        }

        try {
            String response = serialPortManager.sendCommand("RESET");
            log.info("Arduino reseteado: {}", response);
        } catch (ArduinoException e) {
            log.warn("Error reseteando Arduino", e);
        }
    }

    
    private SensorReading parseStateResponse(String response) throws ArduinoException {
        try {
            
            if (response.startsWith("STATUS:") || response.startsWith("MOVING:") 
                || response.startsWith("OK:ARRIVED") || response.startsWith("ERROR:007")) {
                throw new ArduinoException("STATUS_ASYNC_IGNORABLE");
            }
            
            if (!response.startsWith("STATE:")) {
                throw new ArduinoException("Formato de respuesta inválido: " + response);
            }

            String stateData = response.substring(6);
            String[] parts = stateData.split(",");

            if (parts.length < 3) {
                throw new ArduinoException("Respuesta incompleta: " + response);
            }

            int floor           = Integer.parseInt(parts[0].split("=")[1]);
            DoorState doorState = DoorState.valueOf(parts[1].split("=")[1]);

            ElevatorState elevatorState;
            try {
                elevatorState = ElevatorState.valueOf(parts[2].split("=")[1]);
            } catch (IllegalArgumentException e) {
                log.warn("ElevatorState desconocido '{}', usando IDLE", parts[2].split("=")[1]);
                elevatorState = ElevatorState.IDLE;
            }

            Boolean sensorPuertaCerrada = null;
            Boolean sensorPisoDetectado = null;
            boolean sensorDetected = false;

            if (parts.length == 4) {
                
                sensorDetected = "DETECTED".equals(parts[3].split("=")[1]);

            } else if (parts.length >= 5) {
                
                String puertaVal = parts[3].split("=")[1];  
                sensorPuertaCerrada = "CLOSED".equals(puertaVal);

                String pisoVal = parts[4].split("=")[1];   
                sensorPisoDetectado = "AT_FLOOR".equals(pisoVal);

                
                sensorDetected = sensorPisoDetectado;
            }


            return SensorReading.builder()
                    .floor(floor)
                    .doorState(doorState)
                    .elevatorState(elevatorState)
                    .sensorDetected(sensorDetected)
                    .sensorPuertaCerrada(sensorPuertaCerrada)
                    .sensorPisoDetectado(sensorPisoDetectado)
                    .build();

        } catch (Exception e) {
            throw new ArduinoException("Error parseando respuesta: " + response, e);
        }
    }

    
    private void waitForFloor(int targetFloor) {
        int maxRetries = 300;
        int retry      = 0;

        while (retry < maxRetries) {
            try {
                SensorReading reading = readState();

                if (reading.getElevatorState() == ElevatorState.EMERGENCY_STOP) {
                    log.warn("EMERGENCY_STOP detectado durante desplazamiento a piso {}", targetFloor);
                    return;
                }

                if (reading.getFloor() == targetFloor
                        && reading.getElevatorState() == ElevatorState.IDLE) {
                    log.info("Elevador llegó al piso {}", targetFloor);
                    return;
                }

                if (reading.getElevatorState() == ElevatorState.GOING_UP
                        || reading.getElevatorState() == ElevatorState.GOING_DOWN) {
                    log.debug("En transito {} → piso actual: {}, destino: {}",
                            reading.getElevatorState(), reading.getFloor(), targetFloor);
                }

                Thread.sleep(1000);
                retry++;

            } catch (InterruptedException e) {
                log.warn("Polling interrumpido");
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                
                // Si el Arduino mandó un aviso de progreso, simplemente lo ignoramos y seguimos esperando
                if (msg.contains("STATUS_ASYNC_IGNORABLE")) {
                    log.debug("Recibido evento asíncrono del Arduino, continuando polling...");
                } 
                else if (msg.contains("008") || msg.contains("Timeout reaching floor")
                        || msg.contains("007") || msg.contains("Timeout leaving floor")) {
                    log.warn("Error transitorio de Arduino en polling (ignorado): {}", msg);
                    flushInputBuffer();
                } else {
                    log.warn("Error durante polling de estado: {}", msg);
                }
                retry++;
            }
        }

        log.warn("Timeout esperando llegada al piso {}", targetFloor);
    }
    
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Arduino no está inicializado");
        }
    }
}