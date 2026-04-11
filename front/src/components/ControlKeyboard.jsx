import React, { useState } from 'react'
import './ControlKeyboard.css'

function ControlKeyboard({
  requestedFloors,
  onFloorRequest,
  onOpenDoor,
  onCloseDoor,
  doorState,
  isMoving,
  isDoorOpen
}) {
  const floors = [5, 4, 3, 2, 1]
  const [pressedButton, setPressedButton] = useState(null)

  const handleFloorClick = (floor) => {
    if (!isMoving) {
      setPressedButton(floor)
      onFloorRequest(floor)
      setTimeout(() => setPressedButton(null), 200)
      
      // Sonido opcional
      playBeep()
    }
  }

  const handleActionClick = (action) => {
    setPressedButton(action)
    if (action === 'open') {
      onOpenDoor()
    } else if (action === 'close') {
      onCloseDoor()
    }
    setTimeout(() => setPressedButton(null), 200)
    playBeep()
  }

  const playBeep = () => {
    try {
      const audioContext = new (window.AudioContext || window.webkitAudioContext)()
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      oscillator.frequency.value = 800
      oscillator.type = 'sine'
      
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1)
      
      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + 0.1)
    } catch (e) {
      // Audio no disponible
    }
  }

  return (
    <div className="control-keyboard">
      {/* Botones de pisos */}
      <div className="floor-buttons">
        <div className="floor-grid">
          {floors.map((floor) => (
            <button
              key={floor}
              className={`floor-button ${
                requestedFloors.includes(floor) ? 'requested' : ''
              } ${pressedButton === floor ? 'pressed' : ''} ${
                isMoving ? 'disabled' : ''
              }`}
              onClick={() => handleFloorClick(floor)}
              disabled={isMoving}
              aria-label={`Piso ${floor}`}
            >
              <span className="button-glow"></span>
              <span className="button-shadow"></span>
              <span className="button-text">{floor}</span>
              {requestedFloors.includes(floor) && (
                <span className="button-indicator"></span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Botones de acción */}
      <div className="action-buttons">
        <button
          className={`action-button open-button ${
            pressedButton === 'open' ? 'pressed' : ''
          } ${isDoorOpen ? 'active' : ''}`}
          onClick={() => handleActionClick('open')}
          aria-label="Abrir puerta"
        >
          <span className="action-glow"></span>
          <span className="action-shadow"></span>
          <span className="action-text">
            <span className="action-icon">🚪</span>
            <span>ABRIR</span>
          </span>
        </button>

        <button
          className={`action-button close-button ${
            pressedButton === 'close' ? 'pressed' : ''
          }`}
          onClick={() => handleActionClick('close')}
          aria-label="Cerrar puerta"
        >
          <span className="action-glow"></span>
          <span className="action-shadow"></span>
          <span className="action-text">
            <span className="action-icon">✖️</span>
            <span>CERRAR</span>
          </span>
        </button>
      </div>

      {/* Botón de emergencia */}
      <div className="emergency-section">
        <button
          className={`emergency-button ${pressedButton === 'emergency' ? 'pressed' : ''}`}
          onClick={() => handleActionClick('emergency')}
          aria-label="Emergencia"
        >
          <span className="emergency-glow"></span>
          <span className="emergency-shadow"></span>
          <span className="emergency-text">⚠️ EMERGENCIA</span>
        </button>
      </div>

      {/* Indicador de estado */}
      <div className="keyboard-status">
        {isMoving && <div className="status-badge moving">EN MOVIMIENTO</div>}
        {isDoorOpen && <div className="status-badge open">PUERTA ABIERTA</div>}
        {requestedFloors.length > 0 && (
          <div className="status-badge queue">
            {requestedFloors.length} piso(s)
          </div>
        )}
      </div>
    </div>
  )
}

export default ControlKeyboard
