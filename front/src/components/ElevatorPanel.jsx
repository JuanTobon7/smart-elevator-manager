import React, { useState, useEffect } from 'react'
import './ElevatorPanel.css'
import ControlKeyboard from './ControlKeyboard'
import CabinIndicator from './CabinIndicator'

function ElevatorPanel({ elevator, warning, onRequestFloor, onOpenDoor, onCloseDoor, onEmergencyStop }) {
  const [requestedFloors, setRequestedFloors] = useState([])

  useEffect(() => {
    if (!elevator) return
    setRequestedFloors([])
  }, [elevator?.id])

  useEffect(() => {
    if (!elevator) return
    if (elevator.destinationFloor !== null &&
      elevator.currentFloor === elevator.destinationFloor) {
      setRequestedFloors(prev => prev.filter(f => f !== elevator.currentFloor))
    }
  }, [elevator?.currentFloor])

  if (!elevator) {
    return (
      <div className="elevator-panel">
        <div className="no-elevator-selected">
          <p>Selecciona un elevador para comenzar</p>
        </div>
      </div>
    )
  }

  // Si hay warning activo, el movimiento fue rechazado — mostrar IDLE
  const effectiveStatus = warning && (
    elevator.status === 'GOING_UP' ||
    elevator.status === 'GOING_DOWN' ||
    elevator.status === 'MOVING'
  ) ? 'IDLE' : elevator.status

  const doorState = (() => {
    switch (effectiveStatus) {
      case 'DOOR_OPEN':    return 'open'
      case 'DOOR_OPENING': return 'opening'
      case 'DOOR_CLOSING': return 'closing'
      default:             return 'closed'
    }
  })()

  const isMoving =
    effectiveStatus === 'MOVING' ||
    effectiveStatus === 'GOING_UP' ||
    effectiveStatus === 'GOING_DOWN'

  const handleFloorRequest = (floor) => {
    if (!requestedFloors.includes(floor)) {
      setRequestedFloors(prev => [...prev, floor])
    }
    onRequestFloor(floor)
  }

  return (
    <div className="elevator-panel">
      {warning && (
        <div className="panel-warning">
          <span>⚠️ {warning}</span>
        </div>
      )}

      <div className="panel-container">
        <div className="panel-section display-section">
          <CabinIndicator
            currentFloor={elevator.currentFloor}
            elevatorState={effectiveStatus}
            sensorDetected={elevator.sensorDetected ?? false}
            destinationFloor={warning ? null : elevator.destinationFloor}
            sensorPisoDetectado={elevator.sensorPisoDetectado}
            sensorPuertaCerrada={elevator.sensorPuertaCerrada}
          />
        </div>

        <div className="panel-section control-section">
          <ControlKeyboard
            requestedFloors={requestedFloors}
            onFloorRequest={handleFloorRequest}
            onOpenDoor={onOpenDoor}
            onCloseDoor={onCloseDoor}
            onEmergencyStop={onEmergencyStop}
            doorState={doorState}
            isMoving={isMoving}
            isDoorOpen={effectiveStatus === 'DOOR_OPEN'}
          />
        </div>
      </div>

      <div className="panel-status">
        <div className={`status-indicator ${effectiveStatus?.toLowerCase()}`}>
          <span className="status-dot"></span>
          <span className="status-text">{effectiveStatus}</span>
        </div>
      </div>
    </div>
  )
}

export default ElevatorPanel