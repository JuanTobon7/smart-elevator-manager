package co.edu.unillanos.elevator.infrastructure.elevator;

import co.edu.unillanos.elevator.application.port.out.HardwarePort;
import co.edu.unillanos.elevator.domain.model.Elevator;
import co.edu.unillanos.elevator.infrastructure.adapter.ArduinoAdapter;
import co.edu.unillanos.elevator.infrastructure.adapter.SimulatorAdapter;
import co.edu.unillanos.elevator.infrastructure.event.ElevatorEventBroadcaster;
import co.edu.unillanos.elevator.infrastructure.factory.ElevatorOrchestratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crea orquestadores de elevador.
 */
@Component
public class ElevatorOrchestratorFactoryImpl implements ElevatorOrchestratorFactory {
    private static final Logger log = LoggerFactory.getLogger(ElevatorOrchestratorFactoryImpl.class);
    private static final Pattern ELEVATOR_NUMBER_PATTERN = Pattern.compile("(\\d+)$");

    private final ElevatorEventBroadcaster eventBroadcaster;
    private final ObjectProvider<ArduinoAdapter> arduinoAdapterProvider;

    public ElevatorOrchestratorFactoryImpl(
            ElevatorEventBroadcaster eventBroadcaster,
            ObjectProvider<ArduinoAdapter> arduinoAdapterProvider
    ) {
        this.eventBroadcaster = eventBroadcaster;
        this.arduinoAdapterProvider = arduinoAdapterProvider;
    }

    @Override
    public ElevatorOrchestrator create(String elevatorId) {
        HardwareSelection selection = selectHardware(elevatorId);
        log.info("Creando elevador {} con backend {}", elevatorId, selection.backendType());

        return new ElevatorOrchestrator(
                new Elevator(),
                elevatorId,
                selection.hardwarePort(),
                selection.backendType(),
                eventBroadcaster
        );
    }

    private HardwareSelection selectHardware(String elevatorId) {
        ArduinoAdapter arduinoAdapter = arduinoAdapterProvider.getIfAvailable();
        if (isPrimaryElevator(elevatorId) && arduinoAdapter != null && arduinoAdapter.isAvailable()) {
            return new HardwareSelection(arduinoAdapter, "arduino");
        }

        if (isPrimaryElevator(elevatorId)) {
            log.warn("Arduino no disponible para {}. Se usara simulacion.", elevatorId);
        }

        return new HardwareSelection(new SimulatorAdapter(), "virtual");
    }

    private boolean isPrimaryElevator(String elevatorId) {
        Matcher matcher = ELEVATOR_NUMBER_PATTERN.matcher(elevatorId);
        return matcher.find() && Integer.parseInt(matcher.group(1)) == 1;
    }

    private record HardwareSelection(HardwarePort hardwarePort, String backendType) {
    }
}