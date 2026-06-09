import React, { useState, useEffect } from 'react'
import './LCDDisplay.css'

function LCDDisplay({ currentFloor, destinationFloor, status, direction, weight }) {
  const [displayFloor, setDisplayFloor] = useState(currentFloor ?? 1)
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
    const normalized = status?.toUpperCase()

    if (normalized === 'GOING_UP')   return '↑ SUBIENDO'
    if (normalized === 'GOING_DOWN') return '↓ BAJANDO'

    // Fallback: prop direction explícita (compatible con versión anterior)
    switch (direction?.toUpperCase()) {
      case 'UP':   return '↑ SUBIENDO'
      case 'DOWN': return '↓ BAJANDO'
      default:     return '⊗ PARADO'
    }
  }

  // ← MODIFICADO: nuevos casos para GOING_UP, GOING_DOWN y EMERGENCY_STOP
  const getStatusText = () => {
    switch (status?.toUpperCase()) {
      case 'GOING_UP':        return 'SUBIENDO'
      case 'GOING_DOWN':      return 'BAJANDO'
      case 'IDLE':            return 'EN REPOSO'
      case 'DOOR_OPEN':       return 'PUERTA ABIERTA'
      case 'DOOR_OPENING':    return 'ABRIENDO PUERTA'
      case 'DOOR_CLOSING':    return 'CERRANDO PUERTA'
      case 'EMERGENCY_STOP':  return '⚠ EMERGENCIA'
      case 'ERROR':           return '✕ ERROR'
      case 'MOVING':          return 'EN MOVIMIENTO'
      case 'EMERGENCY':       return '⚠ EMERGENCIA'
      default:                return status ?? '---'
    }
  }

  // ← NUEVO: el elevador está en tránsito si sube o baja
  const isMoving = ['GOING_UP', 'GOING_DOWN', 'MOVING'].includes(
    status?.toUpperCase()
  )

  // ← NUEVO: parada de emergencia activa
  const isEmergency = ['EMERGENCY_STOP', 'EMERGENCY'].includes(
    status?.toUpperCase()
  )

  return (
    <div className={`lcd-display ${isEmergency ? 'lcd-emergency' : ''}`}>
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
          {/* ← MODIFICADO: breathing usa isMoving en lugar de status === 'MOVING' */}
          <div className={`direction-arrow ${isMoving ? 'breathing' : ''} ${isEmergency ? 'emergency-blink' : ''}`}>
            {getDirectionArrow()}
          </div>
        </div>

        {/* Separador */}
        <div className="lcd-divider"></div>

        {/* Sección destino */}
        <div className="lcd-section destination-section">
          <div className="lcd-label">DESTINO</div>
          <div className="lcd-value destination-value">
            {/* ← MODIFICADO: ocultar destino si hay emergencia */}
            {isEmergency
              ? 'DETENIDO'
              : destinationFloor
                ? `PISO ${destinationFloor}`
                : '---'}
          </div>
        </div>

        {/* Sección estado */}
        <div className="lcd-section status-section">
          <div className="lcd-label">ESTADO</div>
          {/* ← MODIFICADO: clase CSS dinámica usa el status normalizado */}
          <div className={`lcd-value status-value ${status?.toUpperCase()}`}>
            {getStatusText()}
          </div>
        </div>

        {/* Sección de capacidad */}
        <div className="lcd-section capacity-section">
          <div className="lcd-label">CAPACIDAD</div>
          <div className="capacity-bar">
            <div
              className="capacity-fill"
              style={{ width: `${(weight || 0) * 20}%` }}
            ></div>
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