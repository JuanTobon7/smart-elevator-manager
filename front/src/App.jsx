import React, { useEffect, useState, useRef } from 'react'
import './App.css'
import ElevatorSelector from './components/ElevatorSelector'
import ElevatorPanel from './components/ElevatorPanel'
import ElevatorService from './services/ElevatorService'

function App() {
  const [elevators, setElevators] = useState([])
  const [selectedElevatorId, setSelectedElevatorId] = useState(null)
  const [selectedElevator, setSelectedElevator] = useState(null)
  const [loading, setLoading] = useState(true)
  const [theme, setTheme] = useState('dark')
  const [warning, setWarning] = useState(null)  
  const selectedElevatorIdRef = useRef(selectedElevatorId)

  // Mantenerlo sincronizado cuando cambie
  useEffect(() => {
    selectedElevatorIdRef.current = selectedElevatorId
  }, [selectedElevatorId])

  useEffect(() => {
    const fetchElevators = async () => {
      try {
        const data = await ElevatorService.getElevators()
        setElevators(data)
        if (data.length > 0) {
          setSelectedElevatorId(data[0].id)
        }
      } catch (error) {
        console.error('Error loading elevators:', error)
      } finally {
        setLoading(false)
      }
    }
    fetchElevators()
  }, [])

  useEffect(() => {
    if (elevators.length === 0) return

    const unsubscribers = elevators.map(elevator =>
      
      ElevatorService.subscribeToElevatorUpdates(
        elevator.id,
        (updatedElevator, eventType, errorMessage) => {

          if (eventType === 'VALIDATION_ERROR' || eventType === 'ERROR') {
            if (elevator.id === selectedElevatorIdRef.current) {
              setWarning(errorMessage)
              setTimeout(() => {
                ElevatorService.getElevatorById(elevator.id).then(realState => {
                if (realState) {
                  setElevators(prevs =>
                    prevs.map(e => e.id === realState.id ? realState : e)
                  )
                  setSelectedElevator(realState)
                }
              })
            }, 800)
          }
            return
        }

          if (!updatedElevator) return

          // ← NUEVO: si hay warning activo y el evento es de polling,
          // no pisar el estado hasta que el warning expire
          // El warning indica que el movimiento fue rechazado —
          // el estado real es IDLE, no GOING_UP
          if (eventType === 'POLL' && updatedElevator.status === 'GOING_UP') {
            return
          }

          setWarning(null)
          setElevators(prevs =>
            prevs.map(e => e.id === updatedElevator.id ? updatedElevator : e)
          )
          setSelectedElevator(prev =>
            prev?.id === updatedElevator.id ? updatedElevator : prev
          )
        }
      )
    )

    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe?.())
    }
  }, [elevators.length])

  useEffect(() => {
    if (!selectedElevatorId) return
    ElevatorService.getElevatorById(selectedElevatorId).then(data => {
      setSelectedElevator(data)
    })
  }, [selectedElevatorId])

  const handleSelectElevator = (elevatorId) => {
    setSelectedElevatorId(elevatorId)
    setWarning(null)  // ← limpiar warning al cambiar elevador
  }

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark')
  }

  if (loading) {
    return (
      <div className={`app ${theme}`}>
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Cargando elevadores...</p>
        </div>
      </div>
    )
  }

  return (
    <div className={`app ${theme}`}>
      <header className="app-header">
        <div className="header-content">
          <h1 className="app-title">
            <span className="title-icon">🏢</span>
            Smart Elevator Manager
          </h1>
          <button className="theme-toggle" onClick={toggleTheme}>
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
        </div>

        {/* Banner de advertencia */}
        {warning && (
          <div className="warning-banner">
            <span className="warning-icon">⚠️</span>
            <span className="warning-text">{warning}</span>
            <button className="warning-close" onClick={() => setWarning(null)}>✕</button>
          </div>
        )}
      </header>

      <main className="app-main">
        <aside className="elevator-selector-container">
          <ElevatorSelector
            elevators={elevators}
            selectedId={selectedElevatorId}
            onSelect={handleSelectElevator}
          />
        </aside>

        <section className="elevator-panel-container">
          {selectedElevator ? (
            <ElevatorPanel
              elevator={selectedElevator}
              warning={warning}
              onRequestFloor={(floor) =>
                ElevatorService.requestFloor(selectedElevatorId, floor)
              }
              onOpenDoor={() =>
                ElevatorService.openDoor(selectedElevatorId)
              }
              onCloseDoor={() =>
                ElevatorService.closeDoor(selectedElevatorId)
              }
              onEmergencyStop={() =>
                ElevatorService.emergencyStop(selectedElevatorId)
              }
            />
          ) : (
            <div className="no-elevator-selected">
              <p>Selecciona un elevador</p>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default App