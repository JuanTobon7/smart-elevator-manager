import React, { useState, useEffect, useRef } from 'react'

const styles = `
  @import url('https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Oswald:wght@300;400;600&display=swap');

  .cabin-indicator {
    font-family: 'Oswald', sans-serif;
    background: #0a0a0a;
    border: 2px solid #1a1a1a;
    border-radius: 4px;
    padding: 0;
    width: 100%;
    max-width: 320px;
    margin: 0 auto;
    overflow: hidden;
    box-shadow: inset 0 0 40px rgba(0,0,0,0.8);
  }

  .cabin-header {
    background: #111;
    border-bottom: 1px solid #222;
    padding: 8px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .cabin-header-label {
    font-family: 'Share Tech Mono', monospace;
    font-size: 10px;
    color: #444;
    letter-spacing: 3px;
    text-transform: uppercase;
  }

  .cabin-sensor-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #1a3a1a;
    transition: background 0.15s;
  }
  .cabin-sensor-dot.active {
    background: #00ff41;
    box-shadow: 0 0 6px #00ff41;
  }

  .cabin-floor-display {
    padding: 24px 16px 16px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    gap: 12px;
    min-height: 120px;
    position: relative;
  }

  .cabin-floor-number {
    font-family: 'Share Tech Mono', monospace;
    font-size: 88px;
    line-height: 1;
    font-weight: 400;
    color: #ff6600;
    text-shadow: 0 0 20px rgba(255,102,0,0.4), 0 0 40px rgba(255,102,0,0.15);
    transition: all 0.2s;
    letter-spacing: -4px;
    min-width: 110px;
    text-align: center;
  }

  .cabin-floor-number.moving {
    color: #ffaa00;
    text-shadow: 0 0 20px rgba(255,170,0,0.5), 0 0 40px rgba(255,170,0,0.2);
  }

  .cabin-floor-number.sensor-on {
    color: #00ff41;
    text-shadow: 0 0 20px rgba(0,255,65,0.5), 0 0 40px rgba(0,255,65,0.2);
  }

  .cabin-direction-col {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding-bottom: 12px;
  }

  .cabin-arrow {
    font-size: 22px;
    line-height: 1;
    color: #2a2a2a;
    transition: color 0.2s, text-shadow 0.2s;
    text-align: center;
  }

  .cabin-arrow.up.active {
    color: #ff6600;
    text-shadow: 0 0 10px rgba(255,102,0,0.6);
    animation: arrow-pulse 0.8s ease-in-out infinite;
  }

  .cabin-arrow.down.active {
    color: #ff6600;
    text-shadow: 0 0 10px rgba(255,102,0,0.6);
    animation: arrow-pulse 0.8s ease-in-out infinite;
  }

  @keyframes arrow-pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.3; }
  }

  .cabin-divider {
    height: 1px;
    background: #1a1a1a;
    margin: 0 16px;
  }

  .cabin-status-bar {
    padding: 10px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .cabin-status-text {
    font-family: 'Share Tech Mono', monospace;
    font-size: 11px;
    letter-spacing: 2px;
    text-transform: uppercase;
    color: #444;
    transition: color 0.3s;
  }

  .cabin-status-text.going-up,
  .cabin-status-text.going-down {
    color: #ff6600;
  }

  .cabin-status-text.idle {
    color: #00ff41;
  }

  .cabin-status-text.emergency {
    color: #ff2200;
    animation: blink 0.5s step-end infinite;
  }

  @keyframes blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0; }
  }

  .cabin-floor-label {
    font-family: 'Share Tech Mono', monospace;
    font-size: 10px;
    color: #333;
    letter-spacing: 2px;
  }

  .cabin-sensor-bar {
    padding: 8px 16px 12px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .cabin-sensor-label {
    font-family: 'Share Tech Mono', monospace;
    font-size: 9px;
    color: #333;
    letter-spacing: 1px;
    flex: 1;
  }

  .cabin-sensor-pip {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #1a1a1a;
    border: 1px solid #2a2a2a;
    transition: all 0.1s;
  }

  .cabin-sensor-pip.floor-detected {
    background: #00ff41;
    border-color: #00ff41;
    box-shadow: 0 0 8px rgba(0,255,65,0.8);
  }

  .cabin-segment-row {
    display: flex;
    gap: 4px;
    align-items: center;
  }

  .cabin-seg {
    width: 14px;
    height: 3px;
    background: #1a1a1a;
    border-radius: 1px;
    transition: background 0.15s;
  }

  .cabin-seg.lit {
    background: #ff6600;
    box-shadow: 0 0 4px rgba(255,102,0,0.5);
  }
`

export default function CabinIndicator({
  currentFloor = 1,
  elevatorState = 'IDLE',
  sensorDetected = false,
  destinationFloor = null
}) {
  const [displayFloor, setDisplayFloor] = useState(currentFloor)
  const [flashing, setFlashing] = useState(false)
  const prevFloor = useRef(currentFloor)

  useEffect(() => {
    if (prevFloor.current !== currentFloor) {
      setFlashing(true)
      const t = setTimeout(() => {
        setDisplayFloor(currentFloor)
        setFlashing(false)
        prevFloor.current = currentFloor
      }, 150)
      return () => clearTimeout(t)
    }
  }, [currentFloor])

  const isMoving = elevatorState === 'GOING_UP' || elevatorState === 'GOING_DOWN'
  const isUp = elevatorState === 'GOING_UP'
  const isDown = elevatorState === 'GOING_DOWN'
  const isEmergency = elevatorState === 'EMERGENCY_STOP'
  const isIdle = elevatorState === 'IDLE'

  const statusClass = isMoving
    ? (isUp ? 'going-up' : 'going-down')
    : isEmergency ? 'emergency' : 'idle'

  const statusText = {
    GOING_UP: 'SUBIENDO',
    GOING_DOWN: 'BAJANDO',
    IDLE: 'EN REPOSO',
    DOOR_OPEN: 'PUERTA ABIERTA',
    DOOR_OPENING: 'ABRIENDO',
    DOOR_CLOSING: 'CERRANDO',
    EMERGENCY_STOP: '!! EMERGENCIA !!'
  }[elevatorState] ?? elevatorState

  const floorClass = flashing
    ? ''
    : sensorDetected
    ? 'sensor-on'
    : isMoving
    ? 'moving'
    : ''

  return (
    <>
      <style>{styles}</style>
      <div className="cabin-indicator">
        <div className="cabin-header">
          <span className="cabin-header-label">INDICADOR DE CABINA</span>
          <div className={`cabin-sensor-dot ${sensorDetected ? 'active' : ''}`} title="Sensor de piso" />
        </div>

        <div className="cabin-floor-display">
          <div className="cabin-direction-col">
            <div className={`cabin-arrow up ${isUp ? 'active' : ''}`}>▲</div>
            <div className={`cabin-arrow down ${isDown ? 'active' : ''}`}>▼</div>
          </div>
          <div>
            <div className="cabin-floor-label">PISO</div>
            <div className={`cabin-floor-number ${floorClass}`}>
              {flashing ? displayFloor : displayFloor}
            </div>
          </div>
          {destinationFloor && destinationFloor !== displayFloor && (
            <div style={{ paddingBottom: 12 }}>
              <div className="cabin-floor-label">DEST</div>
              <div style={{
                fontFamily: "'Share Tech Mono', monospace",
                fontSize: 28,
                color: '#333',
                lineHeight: 1,
                textAlign: 'center'
              }}>
                {destinationFloor}
              </div>
            </div>
          )}
        </div>

        <div className="cabin-divider" />

        <div className="cabin-sensor-bar">
          <span className="cabin-sensor-label">SENSOR FC-51</span>
          <div className={`cabin-sensor-pip ${sensorDetected ? 'floor-detected' : ''}`} />
          <span style={{
            fontFamily: "'Share Tech Mono', monospace",
            fontSize: 9,
            color: sensorDetected ? '#00ff41' : '#2a2a2a',
            letterSpacing: 1,
            marginLeft: 4,
            transition: 'color 0.15s'
          }}>
            {sensorDetected ? 'MARCA' : 'LIBRE'}
          </span>
        </div>

        <div className="cabin-divider" />

        <div className="cabin-status-bar">
          <span className={`cabin-status-text ${statusClass}`}>
            {statusText}
          </span>
          <div className="cabin-segment-row">
            {[1,2,3,4,5].map(i => (
              <div
                key={i}
                className={`cabin-seg ${isMoving && i <= 3 ? 'lit' : isIdle ? 'lit' : ''}`}
                style={isMoving ? { animationDelay: `${i * 0.1}s` } : {}}
              />
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
