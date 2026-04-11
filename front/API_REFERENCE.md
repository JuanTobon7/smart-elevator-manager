# API Reference & Backend Integration Guide

## 📋 API Endpoints Esperados

### 1. Obtener Todos los Elevadores

```http
GET /api/elevators
```

**Respuesta (200 OK):**
```json
[
  {
    "id": "elev-1",
    "number": 1,
    "currentFloor": 2,
    "destinationFloor": null,
    "status": "IDLE",
    "direction": "STOPPED",
    "weight": 0,
    "doorStatus": "closed",
    "capacity": 10,
    "maxCapacity": 10
  },
  {
    "id": "elev-2",
    "number": 2,
    "currentFloor": 5,
    "destinationFloor": 3,
    "status": "MOVING",
    "direction": "DOWN",
    "weight": 650,
    "doorStatus": "closed",
    "capacity": 8,
    "maxCapacity": 10
  }
]
```

### 2. Obtener Elevador Específico

```http
GET /api/elevators/{elevatorId}
```

**Parámetros:**
- `elevatorId` (string, path) - ID único del elevador

**Respuesta (200 OK):**
```json
{
  "id": "elev-1",
  "number": 1,
  "currentFloor": 2,
  "destinationFloor": null,
  "status": "IDLE",
  "direction": "STOPPED",
  "weight": 0,
  "doorStatus": "closed",
  "capacity": 10,
  "maxCapacity": 10,
  "previousFloors": [1, 1],
  "lastUpdate": "2026-04-11T10:30:00Z"
}
```

### 3. Solicitar Piso

```http
POST /api/elevators/{elevatorId}/request-floor
Content-Type: application/json

{
  "floor": 5
}
```

**Respuesta (200 OK):**
```json
{
  "status": "accepted",
  "message": "Floor request accepted",
  "estimatedTime": 45,
  "queuePosition": 1,
  "allRequests": [5, 4]
}
```

**Errores:**
```json
// 400 Bad Request
{
  "error": "Invalid floor number",
  "validFloors": [1, 2, 3, 4, 5]
}

// 409 Conflict
{
  "error": "Elevator is in emergency mode"
}
```

### 4. Abrir Puerta

```http
POST /api/elevators/{elevatorId}/open-door
```

**Respuesta (200 OK):**
```json
{
  "status": "success",
  "doorStatus": "opening",
  "message": "Door opening"
}
```

**Errores:**
```json
// 400 Bad Request
{
  "error": "Cannot open door - Elevator is moving"
}
```

### 5. Cerrar Puerta

```http
POST /api/elevators/{elevatorId}/close-door
```

**Respuesta (200 OK):**
```json
{
  "status": "success",
  "doorStatus": "closing",
  "message": "Door closing"
}
```

### 6. Server-Sent Events (SSE) - Actualizaciones en Tiempo Real

```http
GET /api/elevators/{elevatorId}/subscribe
Accept: text/event-stream
```

**Eventos (Event Stream):**

**Event: update**
```
event: update
data: {"id":"elev-1","currentFloor":3,"destinationFloor":5,"status":"MOVING","direction":"UP","weight":450}

event: update
data: {"id":"elev-1","currentFloor":4,"destinationFloor":5,"status":"MOVING","direction":"UP","weight":450}

event: arrived
data: {"id":"elev-1","currentFloor":5,"destinationFloor":5,"status":"IDLE","direction":"STOPPED","doorStatus":"opening"}

event: door-opened
data: {"id":"elev-1","doorStatus":"open","timestamp":"2026-04-11T10:35:30Z"}

event: door-closed
data: {"id":"elev-1","doorStatus":"closed","timestamp":"2026-04-11T10:35:40Z"}

event: error
data: {"id":"elev-1","error":"Elevator malfunction","status":"EMERGENCY"}
```

## 🔄 Estado del Elevador - Estados Válidos

```
IDLE         - En reposo, esperando solicitudes
MOVING       - En movimiento entre pisos
DOOR_OPEN    - Puerta abierta, esperando
DOOR_CLOSING - Puerta cerrándose
DOOR_OPENING - Puerta abriéndose
EMERGENCY    - Modo de emergencia
MAINTENANCE  - En mantenimiento
```

## 🔀 Dirección del Elevador - Valores Válidos

```
UP           - Subiendo
DOWN         - Bajando
STOPPED      - Detenido
```

## 📊 Tipos de Eventos SSE

```
update         - Cambio en posición o estado general
arrived        - Llegó al piso de destino
door-opened    - Puerta se abrió completamente
door-closed    - Puerta se cerró completamente
door-opening   - Puerta comenzó a abrirse
door-closing   - Puerta comenzó a cerrarse
error          - Error o emergencia
maintenance    - Cambio a/desde mantenimiento
overweight     - Peso excedido
floor-request  - Nueva solicitud de piso
```

## 🚨 Códigos de Error HTTP

```
200 OK                   - Solicitud exitosa
400 Bad Request          - Parámetro inválido
401 Unauthorized         - No autenticado
403 Forbidden            - No autorizado
404 Not Found            - Elevador no encontrado
409 Conflict             - Estado inválido para operación
500 Internal Server Error - Error del servidor
503 Service Unavailable  - Servicio no disponible
```

## 💡 Ejemplos de Implementación

### Frontend - Consumir API

```javascript
// En ElevatorService.js
import Axios from 'axios'

const API = axios.create({
  baseURL: process.env.VITE_API_BASE_URL || 'http://localhost:3000/api'
})

// Error handler
API.interceptors.response.use(
  response => response,
  error => {
    console.error('API Error:', error.response?.data)
    return Promise.reject(error)
  }
)

export default API
```

### Backend - Express.js (Ejemplo)

```javascript
// backend/routes/elevators.js
app.get('/api/elevators', async (req, res) => {
  try {
    const elevators = await ElevatorService.getAll()
    res.json(elevators)
  } catch (err) {
    res.status(500).json({ error: err.message })
  }
})

app.get('/api/elevators/:id/subscribe', (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream')
  res.setHeader('Cache-Control', 'no-cache')
  res.setHeader('Connection', 'keep-alive')

  const client = { id: req.params.id, res }
  clients.push(client)

  // Limpiar al desconectar
  req.on('close', () => {
    clients = clients.filter(c => c !== client)
  })

  // Enviar datos iniciales
  const elevator = ElevatorService.get(req.params.id)
  res.write(`data: ${JSON.stringify(elevator)}\n\n`)
})
```

## 📈 Flujo de Datos Esperado

```
Usuario selecciona elevador
    ↓
Frontend: GET /api/elevators/{id} (cargar estado)
    ↓
Frontend: SSE /api/elevators/{id}/subscribe (escuchar cambios)
    ↓
Usuario hace click en piso
    ↓
Frontend: POST /api/elevators/{id}/request-floor { floor: 5 }
    ↓
Backend: Procesa solicitud
    ↓
Backend: Envía eventos SSE con updates
    ↓
Frontend: Recibe eventos y actualiza UI
```

## 🔐 Seguridad - Recomendaciones

1. **Autenticación**: Implementar JWT o similar
   ```javascript
   // Agregar en headers
   Authorization: Bearer {token}
   ```

2. **Rate Limiting**: Limitar solicitudes por IP/usuario
   ```javascript
   app.use(rateLimit({
     windowMs: 15 * 60 * 1000, // 15 minutos
     max: 100
   }))
   ```

3. **CORS**: Configurar adecuadamente
   ```javascript
   app.use(cors({
     origin: process.env.FRONTEND_URL,
     credentials: true
   }))
   ```

4. **Validación**: Validar inputs
   ```javascript
   const { floor } = req.body
   if (!floor || floor < 1 || floor > 5) {
     return res.status(400).json({ error: 'Invalid floor' })
   }
   ```

## 📝 Logging & Monitoring

### Logs Recomendados

```javascript
// Importante registrar:
console.log(`Request: ${method} ${path}`)
console.log(`Event: Elevator ${id} moved to floor ${floor}`)
console.log(`Error: ${errorMessage}`)
```

### Métricas

- Tiempo promedio de respuesta por endpoint
- Número de conexiones SSE activas
- Eventos procesados por segundo
- Errores por tipo

## 🧪 Testing - Ejemplos de Curl

```bash
# Obtener elevadores
curl http://localhost:3000/api/elevators

# Obtener elevador específico
curl http://localhost:3000/api/elevators/elev-1

# Solicitar piso
curl -X POST http://localhost:3000/api/elevators/elev-1/request-floor \
  -H "Content-Type: application/json" \
  -d '{"floor": 5}'

# Abrir puerta
curl -X POST http://localhost:3000/api/elevators/elev-1/open-door

# Cerrar puerta
curl -X POST http://localhost:3000/api/elevators/elev-1/close-door

# SSE (con curl, mostrará stream)
curl http://localhost:3000/api/elevators/elev-1/subscribe
```

## 📦 Requisitos Mínimos

```javascript
// Estructura de Elevador requerida
{
  id: string,                   // Identificador único
  number: number,               // Número del elevador (1, 2, 3...)
  currentFloor: number,          // Piso actual (1-5)
  destinationFloor: number|null, // Próximo piso o null
  status: 'IDLE' | 'MOVING' | 'DOOR_OPEN' | ...,
  direction: 'UP' | 'DOWN' | 'STOPPED',
  weight: number,               // 0-100 (porcentaje)
  doorStatus: 'open' | 'closed' | 'opening' | 'closing' (opcional)
}
```

## ⚡ Performance

### Optimizaciones Recomendadas

1. **SSE**: Batching de eventos (máx 1000ms)
2. **Cache**: Caché de cliente (5min)
3. **Compresión**: Gzip para respuestas
4. **Pagination**: Si hay muchos datos históricos

```javascript
// Ejemplo batching en backend
let batch = []
const flushBatch = () => {
  if (batch.length > 0) {
    broadcast(batchedEvent(batch))
    batch = []
  }
}
setInterval(flushBatch, 1000)
```

---

**Documentación API v1.0**  
**Proyecto**: Smart Elevator Manager  
**Fecha**: 2026-04-11
