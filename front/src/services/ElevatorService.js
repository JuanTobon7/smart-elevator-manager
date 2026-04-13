class ElevatorService {
  static BASE_URL = 'http://localhost:8080/api'

  /**
   * Transforma un objeto de elevador del API al formato esperado por el frontend
   */
  static transformElevator(apiElevator) {
    if (!apiElevator) return null

    // Extraer número del ID (elev-1 → 1)
    const number = parseInt(apiElevator.elevatorId?.split('-')[1] || 0)

    // Mapear dirección: NONE → STOPPED, UP/DOWN sin cambios
    const direction = apiElevator.direction === 'NONE' ? 'STOPPED' : apiElevator.direction

    return {
      id: apiElevator.elevatorId,
      number,
      currentFloor: apiElevator.currentFloor || 1,
      destinationFloor: apiElevator.targetFloor || null,
      status: apiElevator.status || 'IDLE',
      direction,
      weight: 0, // API no envía esto, usar default
      doorStatus: apiElevator.doorStatus?.toLowerCase() || 'closed'
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
    const eventSource = new EventSource(
      `${this.BASE_URL}/elevators/${elevatorId}/subscribe`
    )

    const handleUpdate = (event) => {
      try {
        const parsedData = JSON.parse(event.data)
        console.log(`[SSE] Evento recibido: ${event.type}`, parsedData)
        
        let elevatorData = parsedData

        // Según tu backend (ElevatorEventDTO), la info del elevador viene dentro de "state"
        if (parsedData.state) {
          elevatorData = parsedData.state
          // Aseguramos que tenga el ID en caso de que el DTO 'state' no lo incluya
          if (!elevatorData.elevatorId) {
            elevatorData.elevatorId = parsedData.elevatorId
          }
        } else if (parsedData.data) {
          // Por si acaso viene envuelto en "data" (compatibilidad)
          elevatorData = parsedData.data
        }

        // Transformamos los datos y los enviamos a App.jsx
        const transformedData = this.transformElevator(elevatorData)
        callback(transformedData)

      } catch (error) {
        console.error('Error parsing SSE data:', error)
      }
    }

    const handleError = () => {
      console.error('SSE connection error')
      eventSource.close()
      // Intentar reconectar después de 3 segundos
      setTimeout(() => {
        this.subscribeToElevatorUpdates(elevatorId, callback)
      }, 3000)
    }

    // --- AQUÍ SE ESCUCHAN LOS EVENTOS DEL BACK ---
    eventSource.addEventListener('MOVING', handleUpdate)
    eventSource.addEventListener('ARRIVED', handleUpdate)
    eventSource.addEventListener('DOOR_OPENED', handleUpdate)
    eventSource.addEventListener('DOOR_CLOSED', handleUpdate)
    eventSource.addEventListener('RESET', handleUpdate)
    eventSource.addEventListener('ERROR', handleUpdate)

    // Listener genérico y de errores
    eventSource.onmessage = handleUpdate
    eventSource.onerror = handleError

    // Retornar función para desuscribirse
    return () => {
      eventSource.close()
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
}

export default ElevatorService
