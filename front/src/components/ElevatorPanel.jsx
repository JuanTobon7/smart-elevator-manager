import React, { useState, useEffect } from 'react'
import './ElevatorPanel.css'
import ControlKeyboard from './ControlKeyboard'
import CabinIndicator from './CabinIndicator'
// ← LCDDisplay eliminado

function ElevatorPanel({ elevator, onRequestFloor, onOpenDoor, onCloseDoor }) {
  const [requestedFloors, setRequestedFloors] = useState([])
  const [doorState, setDoorState] = useState('closed')

  if (!elevator) {
    return (
      <div className="elevator-panel">
        <div className="no-elevator-selected">
          <p>Selecciona un elevador para comenzar</p>
        </div>
      </div>
    )
  }

  useEffect(() => {
    setDoorState(elevator.status === 'DOOR_OPEN' ? 'open' : 'closed')
  }, [elevator.status])

  useEffect(() => {
    setRequestedFloors([])
  }, [elevator.id])

  useEffect(() => {
    if (elevator.destinationFloor !== null && elevator.currentFloor === elevator.destinationFloor) {
      setRequestedFloors(prev => prev.filter(floor => floor !== elevator.currentFloor))
    }
  }, [elevator.currentFloor])

  const handleFloorRequest = (floor) => {
    if (!requestedFloors.includes(floor)) {
      setRequestedFloors(prev => [...prev, floor])
    }
    onRequestFloor(floor)
  }

  const handleOpenDoor = () => {
    setDoorState('opening')
    onOpenDoor()
    setTimeout(() => setDoorState('open'), 300)
  }

  const handleCloseDoor = () => {
    setDoorState('closing')
    setTimeout(() => setDoorState('closed'), 1000)
    onCloseDoor()
  }

  return (
    <div className="elevator-panel">
      <div className="panel-container">
        {/* Panel izquierdo - CabinIndicator reemplaza LCDDisplay */}
        <div className="panel-section display-section">
          <CabinIndicator
            currentFloor={elevator.currentFloor}
            elevatorState={elevator.status}
            sensorDetected={elevator.sensorDetected ?? false}
            destinationFloor={elevator.destinationFloor}
          />
        </div>

        {/* Panel derecho - Controles */}
        <div className="panel-section control-section">
          <ControlKeyboard
            requestedFloors={requestedFloors}
            onFloorRequest={handleFloorRequest}
            onOpenDoor={handleOpenDoor}
            onCloseDoor={handleCloseDoor}
            doorState={doorState}
            isMoving={
              elevator.status === 'MOVING' ||
              elevator.status === 'GOING_UP' ||
              elevator.status === 'GOING_DOWN'
            }
            isDoorOpen={elevator.status === 'DOOR_OPEN'}
          />
        </div>
      </div>

      <div className="panel-status">
        <div className={`status-indicator ${elevator.status?.toLowerCase()}`}>
          <span className="status-dot"></span>
          <span className="status-text">{elevator.status}</span>
        </div>
      </div>
    </div>
  )
}

export default ElevatorPanel