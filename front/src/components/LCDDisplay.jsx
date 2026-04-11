import React, { useState, useEffect } from 'react'
import './LCDDisplay.css'

function LCDDisplay({ currentFloor, destinationFloor, status, direction, weight }) {
  const [displayFloor, setDisplayFloor] = useState(currentFloor || 0)
  const [isFlipping, setIsFlipping] = useState(false)

  useEffect(() => {
    if (displayFloor !== currentFloor) {
      setIsFlipping(true)
      const timer = setTimeout(() => {
        setDisplayFloor(currentFloor)
        setIsFlipping(false)
      }, 300)
      return () => clearTimeout(timer)
    }
  }, [currentFloor])

  const getDirectionArrow = () => {
    switch (direction?.toUpperCase()) {
      case 'UP':
        return '↑ SUBIENDO'
      case 'DOWN':
        return '↓ BAJANDO'
      default:
        return '⊗ PARADO'
    }
  }

  const getStatusText = () => {
    switch (status?.toUpperCase()) {
      case 'MOVING':
        return 'EN MOVIMIENTO'
      case 'IDLE':
        return 'EN REPOSO'
      case 'DOOR_OPEN':
        return 'PUERTA ABIERTA'
      case 'DOOR_CLOSING':
        return 'CERRANDO PUERTA'
      case 'EMERGENCY':
        return 'EMERGENCIA'
      default:
        return status
    }
  }

  return (
    <div className="lcd-display">
      {/* Efecto de escaneo */}
      <div className="lcd-scanlines"></div>
      
      <div className="lcd-content">
        {/* Sección del piso actual */}
        <div className="lcd-section current-floor-section">
          <div className="lcd-label">PISO ACTUAL</div>
          <div className={`lcd-value floor-value ${isFlipping ? 'flipping' : ''}`}>
            <span className="floor-bracket">▶</span>
            <span className="floor-number">{displayFloor}</span>
            <span className="floor-bracket">◀</span>
          </div>
        </div>

        {/* Sección de dirección */}
        <div className="lcd-section direction-section">
          <div className={`direction-arrow ${status === 'MOVING' ? 'breathing' : ''}`}>
            {getDirectionArrow()}
          </div>
        </div>

        {/* Separador */}
        <div className="lcd-divider"></div>

        {/* Sección destino */}
        <div className="lcd-section destination-section">
          <div className="lcd-label">DESTINO</div>
          <div className="lcd-value destination-value">
            {destinationFloor ? `PISO ${destinationFloor}` : '---'}
          </div>
        </div>

        {/* Sección estado */}
        <div className="lcd-section status-section">
          <div className="lcd-label">ESTADO</div>
          <div className={`lcd-value status-value ${status?.toUpperCase()}`}>
            {getStatusText()}
          </div>
        </div>

        {/* Sección de capacidad */}
        <div className="lcd-section capacity-section">
          <div className="lcd-label">CAPACIDAD</div>
          <div className="capacity-bar">
            <div className="capacity-fill" style={{ width: `${(weight || 0) * 20}%` }}></div>
          </div>
          <div className="capacity-text">{weight || 0}%</div>
        </div>
      </div>

      {/* Marco exterior */}
      <div className="lcd-frame"></div>
    </div>
  )
}

export default LCDDisplay
