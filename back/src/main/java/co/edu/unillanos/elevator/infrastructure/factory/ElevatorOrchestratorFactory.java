package co.edu.unillanos.elevator.infrastructure.factory;

import co.edu.unillanos.elevator.infrastructure.elevator.ElevatorOrchestrator;

public interface ElevatorOrchestratorFactory {
    ElevatorOrchestrator create(String elevatorId);
}