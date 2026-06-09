class ElevatorService {
  static BASE_URL = 'http://localhost:8080/api'

  /**
   * Transforma un objeto de elevador del API al formato esperado por el frontend
   */
  static transformElevator(apiElevator) {
    if (!apiElevator) return null

    const number = parseInt(apiElevator.elevatorId?.split('-')[1] || 0)
    const direction = apiElevator.direction === 'NONE' ? 'STOPPED' : apiElevator.direction

    return {
      id: apiElevator.elevatorId,
      number,
      currentFloor: apiElevator.currentFloor || 1,
      destinationFloor: apiElevator.targetFloor || null,
      status: apiElevator.status || 'IDLE',
      direction,
      weight: 0,
      doorStatus: apiElevator.doorStatus?.toLowerCase() || 'closed',
      sensorDetected: apiElevator.sensorDetected ?? false,
      sensorPuertaCerrada: apiElevator.sensorPuertaCerrada ?? null,
      sensorPisoDetectado: apiElevator.sensorPisoDetectado ?? null

    }
  }

  /**
   * Obtiene la lista de elevadores disponibles
   */
  static async getElevators() {
    try {
      const response = await fetch(`${this.BASE_URL}/elevators`)
      if (!response.ok) throw new Error('Failed to fetch elevators')
      const json = await response.json()

      // La API devuelve: { success, message, data: { elev-1: {...}, elev-2: {...} }, timestamp }
      // Transformar objeto a array y mapear campos
      if (json.data && typeof json.data === 'object') {
        const elevators = Object.values(json.data).map(elevator =>
          this.transformElevator(elevator)
        )
        return elevators
      }

      // Si es un array, transformar cada elemento
      if (Array.isArray(json)) {
        return json.map(elevator => this.transformElevator(elevator))
      }

      // Si es un objeto single elevator
      return [this.transformElevator(json)]
    } catch (error) {
      console.error('Error fetching elevators:', error)
      return this.getMockElevators()
    }
  }

  /**
   * Obtiene el estado actual de un elevador específico
   */
  static async getElevatorById(elevatorId) {
    try {
      const response = await fetch(`${this.BASE_URL}/elevators/${elevatorId}`)
      if (!response.ok) throw new Error('Failed to fetch elevator')
      const json = await response.json()

      // Transformar según sea array o objeto
      if (Array.isArray(json)) {
        return this.transformElevator(json[0])
      }

      // Si es un objeto wrapped en { data: {...} }
      if (json.data) {
        return this.transformElevator(json.data)
      }

      return this.transformElevator(json)
    } catch (error) {
      console.error('Error fetching elevator:', error)
      return this.getMockElevator(elevatorId)
    }
  }

  /**
   * Solicita que el elevador se dirija a un piso específico
   */
  static async requestFloor(elevatorId, floor) {
    try {
      const response = await fetch(
        `${this.BASE_URL}/elevators/${elevatorId}/request-floor`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ floor })
        }
      )
      if (!response.ok) throw new Error('Failed to request floor')
      return await response.json()
    } catch (error) {
      console.error('Error requesting floor:', error)
    }
  }

  /**
   * Abre la puerta del elevador
   */
  static async openDoor(elevatorId) {
    try {
      const response = await fetch(
        `${this.BASE_URL}/elevators/${elevatorId}/open-door`,
        { method: 'POST' }
      )
      if (!response.ok) throw new Error('Failed to open door')
      return await response.json()
    } catch (error) {
      console.error('Error opening door:', error)
    }
  }

  /**
   * Cierra la puerta del elevador
   */
  static async closeDoor(elevatorId) {
    try {
      const response = await fetch(
        `${this.BASE_URL}/elevators/${elevatorId}/close-door`,
        { method: 'POST' }
      )
      if (!response.ok) throw new Error('Failed to close door')
      return await response.json()
    } catch (error) {
      console.error('Error closing door:', error)
    }
  }

  /**
   * Se suscribe a actualizaciones en tiempo real del elevador via SSE
   * Retorna una función para cancelar la suscripción
   */
  static subscribeToElevatorUpdates(elevatorId, callback) {
    let eventSource = null
    let pollInterval = null
    let cancelled = false
    let isMoving = false

    const startPolling = () => {
      if (pollInterval) return
      console.log(`[SSE] Iniciando polling para ${elevatorId}`)
      pollInterval = setInterval(async () => {
        if (cancelled) return
        try {
          const elevator = await this.getElevatorById(elevatorId)
          if (elevator) {
            callback(elevator, 'POLL', null)
            if (elevator.status === 'IDLE' || elevator.status === 'ARRIVED') {
              stopPolling()
              connect()
            }
          }
        } catch (e) {
          console.warn('[Poll] Error:', e)
        }
      }, 1500)
    }

    const stopPolling = () => {
      if (pollInterval) {
        clearInterval(pollInterval)
        pollInterval = null
      }
    }

    const connect = () => {
      if (cancelled) return
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }

      console.log(`[SSE] Conectando a ${elevatorId}`)
      eventSource = new EventSource(`${this.BASE_URL}/elevators/${elevatorId}/subscribe`)

      const handleUpdate = (event) => {
        try {
          const parsedData = JSON.parse(event.data)
          console.log(`[SSE] Evento recibido: ${event.type}`, parsedData)

          if (event.type === 'VALIDATION_ERROR' || event.type === 'ERROR') {
            const errorMessage = parsedData.message || 'Error desconocido'
            stopPolling()  
            callback(null, event.type, errorMessage)
            return
          }

          let elevatorData = parsedData
          if (parsedData.state) {
            elevatorData = parsedData.state
            if (!elevatorData.elevatorId) elevatorData.elevatorId = parsedData.elevatorId
          } else if (parsedData.data) {
            elevatorData = parsedData.data
          }

          const transformed = this.transformElevator(elevatorData)
          callback(transformed, event.type, null)

          if (event.type === 'MOVING') startPolling()
          if (event.type === 'ARRIVED') stopPolling()

        } catch (error) {
          console.error('[SSE] Error parseando:', error)
        }
      }

      const handleError = () => {
        console.warn('[SSE] Conexión perdida, activando polling...')
        eventSource.close()
        eventSource = null
        // Si se cae durante movimiento, el polling sigue funcionando
        // Si no hay polling activo, reconectar SSE después de 3s
        if (!pollInterval) {
          setTimeout(() => { if (!cancelled) connect() }, 3000)
        } else {
          // Reconectar SSE cuando termine el movimiento (lo hace stopPolling → connect)
          console.log('[SSE] Polling activo, SSE se reconectará al llegar')
        }
      }

      eventSource.addEventListener('MOVING',           handleUpdate)
      eventSource.addEventListener('ARRIVED',          handleUpdate)
      eventSource.addEventListener('DOOR_OPENED',      handleUpdate)
      eventSource.addEventListener('DOOR_CLOSED',      handleUpdate)
      eventSource.addEventListener('RESET',            handleUpdate)
      eventSource.addEventListener('ERROR',            handleUpdate)
      eventSource.addEventListener('VALIDATION_ERROR', handleUpdate)
      eventSource.onmessage = handleUpdate
      eventSource.onerror   = handleError
    }

    connect()

    // Retornar función de cancelación
    return () => {
      cancelled = true
      stopPolling()
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
    }
  }

  /**
   * Datos mock para desarrollo/testing
   */
  static getMockElevators() {
    return [
      {
        id: 'elev-1',
        number: 1,
        currentFloor: 2,
        destinationFloor: null,
        status: 'IDLE',
        direction: 'STOPPED',
        weight: 0
      },
      {
        id: 'elev-2',
        number: 2,
        currentFloor: 5,
        destinationFloor: 3,
        status: 'MOVING',
        direction: 'DOWN',
        weight: 65
      },
      {
        id: 'elev-3',
        number: 3,
        currentFloor: 1,
        destinationFloor: 4,
        status: 'MOVING',
        direction: 'UP',
        weight: 40
      }
    ]
  }

  static getMockElevator(elevatorId) {
    const elevators = this.getMockElevators()
    return elevators.find(e => e.id === elevatorId) || elevators[0]
  }

  static async emergencyStop(elevatorId) {
    try {
      const response = await fetch(
        `${this.BASE_URL}/elevators/${elevatorId}/emergency-stop`,
        { method: 'POST' }
      )
      if (!response.ok) throw new Error('Failed to trigger emergency stop')
      return await response.json()
    } catch (error) {
      console.error('Error triggering emergency stop:', error)
    }
  }
}

export default ElevatorService
