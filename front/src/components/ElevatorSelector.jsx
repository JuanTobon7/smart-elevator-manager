import React from 'react'
import './ElevatorSelector.css'

function ElevatorSelector({ elevators, selectedId, onSelect }) {
  // Validar que elevators sea un array
  if (!Array.isArray(elevators)) {
    console.warn('ElevatorSelector: elevators no es un array', elevators)
    return (
      <div className="elevator-selector">
        <h2 className="selector-title">Elevadores</h2>
        <div className="elevator-cards">
          <p style={{ color: 'var(--text-muted)' }}>Cargando elevadores...</p>
        </div>
      </div>
    )
  }

  const getDirectionIcon = (direction) => {
    switch (direction?.toUpperCase()) {
      case 'UP':
        return '↑'
      case 'DOWN':
        return '↓'
      default:
        return '⊗'
    }
  }

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'MOVING':
        return 'moving'
      case 'IDLE':
        return 'idle'
      case 'DOOR_OPEN':
        return 'door-open'
      case 'EMERGENCY':
        return 'emergency'
      default:
        return 'idle'
    }
  }

  return (
    <div className="elevator-selector">
      <h2 className="selector-title">Elevadores</h2>
      <div className="elevator-cards">
        {elevators.map((elevator, index) => (
          <div
            key={elevator.id}
            className={`elevator-card ${selectedId === elevator.id ? 'selected' : ''}`}
            onClick={() => onSelect(elevator.id)}
            style={{ animationDelay: `${index * 0.1}s` }}
          >
            <div className="card-glow"></div>
            
            <div className="card-header">
              <span className="elevator-id">Elevator {elevator.number}</span>
              <span className={`status-badge ${getStatusColor(elevator.status)}`}>
                {elevator.status}
              </span>
            </div>

            <div className="card-content">
              <div className="elevator-display">
                <div className="elevator-visual">
                  <svg viewBox="0 0 100 200" className="elevator-icon">
                    <rect x="20" y="20" width="60" height="140" fill="none" stroke="currentColor" strokeWidth="2" rx="4" />
                    <circle
                      cx="50"
                      cy={60 + (elevator.currentFloor || 1) * 15}
                      r="12"
                      fill="currentColor"
                      className={elevator.status === 'MOVING' ? 'moving-indicator' : ''}
                    />
                    <line x1="25" y1="170" x2="75" y2="170" stroke="currentColor" strokeWidth="2" />
                  </svg>
                </div>

                <div className="floor-info">
                  <div className="current-floor">
                    <span className="floor-label">Piso</span>
                    <span className="floor-number">
                      {elevator.currentFloor || 0}
                    </span>
                  </div>
                  <div className="direction-icon" title={elevator.direction}>
                    {getDirectionIcon(elevator.direction)}
                  </div>
                </div>
              </div>
            </div>

            <div className="card-footer">
              <span className="destination">
                {elevator.destinationFloor ? `→ Piso ${elevator.destinationFloor}` : 'En reposo'}
              </span>
            </div>

            {selectedId === elevator.id && (
              <div className="selection-indicator">
                <div className="selection-line"></div>
                <div className="selection-dot"></div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

export default ElevatorSelector
