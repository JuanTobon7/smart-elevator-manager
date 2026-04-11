package co.edu.unillanos.elevator.infrastructure.adapter;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.model.SensorReading;

/**
 * Adaptador para comunicación con Arduino real
 * Activo solo con perfil "arduino"
 * 
 * Por ahora es un stub que lanza UnsupportedOperationException.
 * TODO: Implementar comunicación serial con jSerialComm
 * TODO: Configurar baudrate, puerto serie, timeout
 * TODO: Implementar protocolo de comunicación con firmware Arduino
 */
@Component
@Profile("arduino")
public class ArduinoAdapter implements HardwarePort {

    private static final Logger log = LoggerFactory.getLogger(ArduinoAdapter.class);

    // TODO: inyectar puerto serial configurado en ElevatorConfig
    // private SerialPort serialPort;

    @Override
    public void executeCommand(String command) {
        log.warn("ArduinoAdapter: executeCommand no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    @Override
    public SensorReading readState() throws InterruptedException {
        log.warn("ArduinoAdapter: readState no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    @Override
    public void moveToFloor(int floor) {
        log.warn("ArduinoAdapter: moveToFloor no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    @Override
    public void openDoor() {
        log.warn("ArduinoAdapter: openDoor no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    @Override
    public void closeDoor() {
        log.warn("ArduinoAdapter: closeDoor no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    @Override
    public void reset() {
        log.warn("ArduinoAdapter: reset no implementado");
        throw new UnsupportedOperationException(
            "ArduinoAdapter aún no implementado. Use perfil 'simulator' para desarrollo."
        );
    }

    // TODO: Implementar métodos privados para:
    // - Abrir conexión serial al puerto Arduino
    // - Enviar comandos al firmware Arduino
    // - Leer respuestas del hardware
    // - Parsear formato de datos del protocolo
    // - Manejar timeout y errores de comunicación
}
