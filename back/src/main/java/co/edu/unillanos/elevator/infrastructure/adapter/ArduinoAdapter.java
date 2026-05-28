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

/**
 * Adaptador para comunicación con Arduino real.
 */
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

        try {
            String response = serialPortManager.sendCommand("READ_STATE");
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
        ensureInitialized();

        if (floor < 1 || floor > 10) {
            log.error("Piso fuera de rango: {}", floor);
            throw new IllegalArgumentException("Piso debe estar entre 1 y 10");
        }

        try {
            log.info("Solicitando movimiento a piso {} al Arduino", floor);
            String response = serialPortManager.sendCommand("MOVE_TO_FLOOR " + floor);

            if (response.contains("MOVING")) {
                log.info("Elevador moviéndose al piso {}", floor);
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
                log.info("Puerta del elevador abriéndose");
                Thread.sleep(1000);
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
                log.info("Puerta del elevador cerrándose");
                Thread.sleep(800);
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
            if (!response.startsWith("STATE:")) {
                throw new ArduinoException("Formato de respuesta inválido: " + response);
            }

            String stateData = response.substring(6);
            String[] parts = stateData.split(",");

            if (parts.length < 3) {
                throw new ArduinoException("Respuesta incompleta: " + response);
            }

            int floor = Integer.parseInt(parts[0].split("=")[1]);
            DoorState doorState = DoorState.valueOf(parts[1].split("=")[1]);
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

    private void waitForFloor(int targetFloor) {
        int maxRetries = 60;
        int retry = 0;

        while (retry < maxRetries) {
            try {
                SensorReading reading = readState();

                if (reading.getFloor() == targetFloor &&
                        reading.getElevatorState() == ElevatorState.IDLE) {
                    log.info("Elevador llegó al piso {}", targetFloor);
                    return;
                }

                Thread.sleep(1000);
                retry++;
            } catch (InterruptedException e) {
                log.warn("Polling interrumpido");
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.warn("Error durante polling de estado: {}", e.getMessage());
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
