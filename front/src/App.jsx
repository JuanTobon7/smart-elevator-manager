import React, { useEffect, useState } from 'react'
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

  // Cargar elevadores disponibles
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

  // Suscribirse a TODOS los elevadores
  useEffect(() => {
    if (elevators.length === 0) return

    const unsubscribers = elevators.map(elevator =>
      ElevatorService.subscribeToElevatorUpdates(
        elevator.id,
        (updatedElevator) => {
          console.log("¡Llegó un evento del servidor!", updatedElevator)
          // Actualizar en la lista de elevadores
          setElevators(prevs =>
            prevs.map(e => e.id === updatedElevator.id ? updatedElevator : e)
          )
          // Si es el elevador seleccionado, actualizar también selectedElevator
          setSelectedElevator(prev => 
            prev?.id === updatedElevator.id ? updatedElevator : prev
          )
        }
      )
    )

    // Cleanup: desuscribirse de todos al desmontar
    return () => {
      unsubscribers.forEach(unsubscribe => unsubscribe?.())
    }
  }, [elevators.length])

  // Al cambiar elevador seleccionado, cargar su estado actual
  useEffect(() => {
    if (!selectedElevatorId) return

    ElevatorService.getElevatorById(selectedElevatorId).then(data => {
      setSelectedElevator(data)
    })
  }, [selectedElevatorId])

  const handleSelectElevator = (elevatorId) => {
    setSelectedElevatorId(elevatorId)
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
          <button className="theme-toggle" onClick={toggleTheme} title="Cambiar tema">
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>
        </div>
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
              onRequestFloor={(floor) => 
                ElevatorService.requestFloor(selectedElevatorId, floor)
              }
              onOpenDoor={() => 
                ElevatorService.openDoor(selectedElevatorId)
              }
              onCloseDoor={() => 
                ElevatorService.closeDoor(selectedElevatorId)
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
