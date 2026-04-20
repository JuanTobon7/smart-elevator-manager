package co.edu.unillanos.elevator.infrastructure.adapter;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Gestor de comunicación serial con Arduino
 * 
 * Responsabilidades:
 * - Conectar/desconectar puerto serial
 * - Enviar comandos al Arduino
 * - Leer respuestas del Arduino
 * - Manejar errores y timeouts
 * - Thread-safe
 */
public class SerialPortManager {
    
    private static final Logger log = LoggerFactory.getLogger(SerialPortManager.class);
    
    private static final int BAUD_RATE = 115200;
    private static final int TIMEOUT_MS = 5000;
    private static final int RETRY_COUNT = 3;
    
    private SerialPort serialPort;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean connected = false;
    
    /**
     * Conecta al puerto serial especificado
     * 
     * @param portName Nombre del puerto (ej: "COM3", "/dev/ttyUSB0")
     * @return true si la conexión fue exitosa
     */
    public boolean connect(String portName) {
        lock.writeLock().lock();
        try {
            if (connected) {
                log.warn("Ya hay una conexión activa");
                return true;
            }
            
            // Obtener puerto por nombre
            serialPort = SerialPort.getCommPort(portName);
            
            if (serialPort == null) {
                log.error("Puerto serial no encontrado: {}", portName);
                return false;
            }
            
            // Configurar parámetros
            serialPort.setBaudRate(BAUD_RATE);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            
            // Abrir puerto
            if (!serialPort.openPort()) {
                log.error("No se pudo abrir el puerto: {}", portName);
                return false;
            }
            
            connected = true;
            log.info("Conectado a Arduino en puerto: {} @ {} bps", portName, BAUD_RATE);
            
            // Limpiar buffers
            serialPort.clearDTR();
            serialPort.clearRTS();
            
            return true;
            
        } catch (Exception e) {
            log.error("Error al conectar a Arduino", e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Desconecta del puerto serial
     */
    public void disconnect() {
        lock.writeLock().lock();
        try {
            if (serialPort != null && connected) {
                serialPort.closePort();
                connected = false;
                log.info("Desconectado de Arduino");
            }
        } catch (Exception e) {
            log.error("Error al desconectar de Arduino", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Verifica si hay conexión activa
     */
    public boolean isConnected() {
        return connected && serialPort != null && serialPort.isOpen();
    }
    
    /**
     * Envía un comando al Arduino y espera respuesta
     * 
     * @param command Comando a enviar (sin incluir \n)
     * @return Respuesta del Arduino
     * @throws IllegalStateException si no hay conexión
     * @throws ArduinoTimeoutException si se agota el timeout
     * @throws ArduinoException si Arduino devuelve error
     */
    public String sendCommand(String command) throws ArduinoException {
        if (!isConnected()) {
            throw new IllegalStateException("No hay conexión con Arduino");
        }
        
        lock.readLock().lock();
        try {
            // Agregar salto de línea si no está presente
            String cmdToSend = command.endsWith("\n") ? command : command + "\n";
            
            log.debug("→ Enviando comando: {}", command);
            
            // Enviar comando
            byte[] cmdBytes = cmdToSend.getBytes(StandardCharsets.UTF_8);
            serialPort.writeBytes(cmdBytes, cmdBytes.length);
            
            // Leer respuesta con reintentos
            for (int attempt = 0; attempt < RETRY_COUNT; attempt++) {
                String response = readResponse();
                if (response != null) {
                    log.debug("← Respuesta recibida: {}", response);
                    
                    // Verificar si es error
                    if (response.startsWith("ERROR:")) {
                        throw parseError(response);
                    }
                    
                    return response;
                }
            }
            
            throw new ArduinoTimeoutException(
                "Timeout esperando respuesta para comando: " + command
            );
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Lee una línea de respuesta del Arduino
     * 
     * @return Línea leída o null si timeout
     */
    private String readResponse() throws ArduinoException {
        long startTime = System.currentTimeMillis();
        StringBuilder response = new StringBuilder();
        
        while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
            if (serialPort.bytesAvailable() > 0) {
                byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                
                if (numRead > 0) {
                    String chunk = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                    response.append(chunk);
                    
                    // Si encontramos \n, retornar la línea
                    int newlineIndex = response.indexOf("\n");
                    if (newlineIndex >= 0) {
                        return response.substring(0, newlineIndex).trim();
                    }
                }
            }
            
            try {
                Thread.sleep(10); // Pequeña pausa para evitar busy-waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ArduinoException("Lectura interrumpida", e);
            }
        }
        
        return null; // Timeout
    }
    
    /**
     * Parsea un mensaje de error del Arduino
     * 
     * @param errorMsg Mensaje de error del Arduino (ej: "ERROR:003:Floor out of range")
     * @return ArduinoException con los detalles del error
     */
    private ArduinoException parseError(String errorMsg) {
        try {
            String[] parts = errorMsg.split(":", 3);
            if (parts.length >= 3) {
                String code = parts[1];
                String message = parts[2];
                return new ArduinoException(message + " (código: " + code + ")");
            }
        } catch (Exception e) {
            log.warn("No se pudo parsear error de Arduino: {}", errorMsg);
        }
        return new ArduinoException("Error desconocido de Arduino: " + errorMsg);
    }
    
    /**
     * Obtiene lista de puertos seriales disponibles
     * 
     * @return Array de nombres de puertos
     */
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }
    
    /**
     * Excepción para errores de comunicación con Arduino
     */
    public static class ArduinoException extends Exception {
        public ArduinoException(String message) {
            super(message);
        }
        
        public ArduinoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Excepción para timeout de Arduino
     */
    public static class ArduinoTimeoutException extends ArduinoException {
        public ArduinoTimeoutException(String message) {
            super(message);
        }
    }

    public int bytesAvailable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bytesAvailable'");
    }

    public void readBytes(byte[] discard, int length) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readBytes'");
    }
}
