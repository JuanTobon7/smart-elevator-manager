import React, { useState, useEffect } from 'react'
import './ElevatorPanel.css'
import LCDDisplay from './LCDDisplay'
import ControlKeyboard from './ControlKeyboard'

function ElevatorPanel({ elevator, onRequestFloor, onOpenDoor, onCloseDoor }) {
  const [requestedFloors, setRequestedFloors] = useState([])
  const [doorState, setDoorState] = useState('closed')

  // Validar que elevator está definido
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

  // Limpiar pisos solicitados cuando cambia el elevador seleccionado
  useEffect(() => {
    setRequestedFloors([])
  }, [elevator.id])

  // Limpiar pisos solicitados cuando el elevador llega a su destino
  useEffect(() => {
    if (elevator.destinationFloor !== null && elevator.currentFloor === elevator.destinationFloor) {
      // El elevador llegó a su piso destino - limpiar pisos solicitados
      setRequestedFloors(prev => {
        const updated = prev.filter(floor => floor !== elevator.currentFloor)
        return updated
      })
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
        {/* Panel izquierdo - Display */}
        <div className="panel-section display-section">
          <LCDDisplay
            currentFloor={elevator.currentFloor}
            destinationFloor={elevator.destinationFloor}
            status={elevator.status}
            direction={elevator.direction}
            weight={elevator.weight}
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
            isMoving={elevator.status === 'MOVING'}
            isDoorOpen={elevator.status === 'DOOR_OPEN'}
          />
        </div>
      </div>

      {/* Indicador de estado */}
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
