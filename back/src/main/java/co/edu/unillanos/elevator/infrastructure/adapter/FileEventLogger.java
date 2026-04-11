package co.edu.unillanos.elevator.infrastructure.adapter;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import co.edu.unillanos.elevator.application.port.out.EventLogger;
import co.edu.unillanos.elevator.domain.model.SensorReading;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Adaptador para logging de eventos en archivo
 */
@Component
public class FileEventLogger implements EventLogger {

    private static final Logger log = LoggerFactory.getLogger(FileEventLogger.class);
    private static final String LOG_FILE = "elevator.log";
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void logEvent(String eventType, String details, SensorReading reading) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logLine = String.format(
            "%s | %-8s | %s | state=%s | door=%s | floor=%d%n",
            timestamp,
            eventType,
            details,
            reading.getElevatorState(),
            reading.getDoorState(),
            reading.getFloor()
        );
        
        writeToFile(logLine);
        log.debug("Evento registrado: {}", eventType);
    }

    @Override
    public void logError(String errorMessage) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logLine = String.format(
            "%s | %-8s | msg=%s%n",
            timestamp,
            "ERROR",
            errorMessage
        );
        
        writeToFile(logLine);
        log.debug("Error registrado: {}", errorMessage);
    }

    private void writeToFile(String line) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.print(line);
            writer.flush();
        } catch (IOException e) {
            log.error("Error al escribir en {}: {}", LOG_FILE, e.getMessage());
        }
    }
}
