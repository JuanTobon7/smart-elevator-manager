# 🔄 Transformación de Datos - API ↔ Frontend

## ⚠️ Problema Encontrado

El backend devuelve una estructura diferente a la que el frontend esperaba. Se implementó una capa de transformación en `ElevatorService.js` para manejar esto automáticamente.

---

## 📊 Formato API vs Frontend

### ❌ Formato que envía el API (Backend)

```json
{
  "success": true,
  "message": "Elevadores obtenidos exitosamente",
  "data": {
    "elev-1": {
      "elevatorId": "elev-1",
      "currentFloor": 1,
      "targetFloor": 1,
      "status": "IDLE",
      "direction": "NONE",
      "doorStatus": "CLOSED",
      "timestamp": 1775940368110
    },
    "elev-2": { ... },
    "elev-3": { ... }
  },
  "timestamp": 1775940368110
}
```

### ✅ Formato que el Frontend espera (transformado)

```json
[
  {
    "id": "elev-1",
    "number": 1,
    "currentFloor": 1,
    "destinationFloor": 1,
    "status": "IDLE",
    "direction": "STOPPED",
    "weight": 0,
    "doorStatus": "closed"
  },
  {
    "id": "elev-2",
    "number": 2,
    "currentFloor": 1,
    "destinationFloor": 1,
    "status": "IDLE",
    "direction": "STOPPED",
    "weight": 0,
    "doorStatus": "closed"
  }
]
```

---

## 🔧 Mapeo de Campos

| Campo API | Campo Frontend | Transformación |
|-----------|----------------|-----------------|
| `elevatorId` | `id` | Copia directa |
| Extraído de ID | `number` | `elev-1` → `1` |
| `currentFloor` | `currentFloor` | Copia directa |
| `targetFloor` | `destinationFloor` | Renombra |
| `status` | `status` | Copia directa (IDLE, MOVING, etc) |
| `direction` | `direction` | `NONE` → `STOPPED`, `UP` → `UP`, `DOWN` → `DOWN` |
| N/A | `weight` | Default `0` (API no provee) |
| `doorStatus` | `doorStatus` | Convierte a minúsculas |

---

## 📝 Transformación de Estructura

### API devuelve: Objeto anidado
```javascript
{
  "data": {
    "elev-1": {...},
    "elev-2": {...},
    "elev-3": {...}
  }
}
```

### Frontend recibe: Array lineal
```javascript
[
  {...},
  {...},
  {...}
]
```

---

## 🛠️ Función de Transformación

```javascript
// En ElevatorService.js
static transformElevator(apiElevator) {
  if (!apiElevator) return null

  const number = parseInt(apiElevator.elevatorId?.split('-')[1] || 0)
  const direction = apiElevator.direction === 'NONE' ? 'STOPPED' : apiElevator.direction

  return {
    id: apiElevator.elevatorId,          // elev-1
    number,                              // 1
    currentFloor: apiElevator.currentFloor || 1,
    destinationFloor: apiElevator.targetFloor || null,
    status: apiElevator.status || 'IDLE',
    direction,                           // NONE → STOPPED
    weight: 0,
    doorStatus: apiElevator.doorStatus?.toLowerCase() || 'closed'
  }
}
```

---

## 📍 Puntos donde se aplica Transformación

### 1️⃣ GET /api/elevators (obtener lista)

```javascript
// Antes (API)
{
  "data": {
    "elev-1": {...},
    "elev-2": {...}
  }
}

// Después (Frontend)
[
  {id: "elev-1", number: 1, ...},
  {id: "elev-2", number: 2, ...}
]
```

### 2️⃣ GET /api/elevators/{id} (obtener específico)

```javascript
// Antes (API)
{
  "elevatorId": "elev-1",
  "currentFloor": 2,
  ...
}

// Después (Frontend)
{
  "id": "elev-1",
  "number": 1,
  "currentFloor": 2,
  ...
}
```

### 3️⃣ GET /api/elevators/{id}/subscribe (SSE - tiempo real)

```javascript
// Antes (API stream)
event: update
data: {"elevatorId":"elev-1","currentFloor":2,"direction":"UP",...}

// Después (Frontend recibe)
{
  "id": "elev-1",
  "number": 1,
  "currentFloor": 2,
  "direction": "UP",
  ...
}
```

---

## 🚀 Manejo Inteligente de Variables Estructuras

El servicio es **flexible** y maneja múltiples formatos:

### ✅ Soporta Objeto Anidado
```javascript
{
  "data": {
    "elev-1": {...}
  }
}
```

### ✅ Soporta Array Directo
```javascript
[
  {"elevatorId": "elev-1", ...},
  {"elevatorId": "elev-2", ...}
]
```

### ✅ Soporta Objeto Simple
```javascript
{
  "elevatorId": "elev-1",
  ...
}
```

---

## 🛡️ Validación y Seguridad

El servicio incluye:
- ✅ Validación de tipos
- ✅ Valores por defecto
- ✅ Null checks
- ✅ Manejo de errores
- ✅ Fallback a mock data si falla API

---

## 📊 Ejemplo Completo

### Request Inicial

```javascript
// App.jsx
const data = await ElevatorService.getElevators()
```

### API Response

```json
{
  "success": true,
  "data": {
    "elev-1": {
      "elevatorId": "elev-1",
      "currentFloor": 1,
      "targetFloor": 1,
      "status": "IDLE",
      "direction": "NONE",
      "doorStatus": "CLOSED",
      "timestamp": 1775940368110
    },
    "elev-2": {
      "elevatorId": "elev-2",
      "currentFloor": 1,
      "targetFloor": 1,
      "status": "IDLE",
      "direction": "NONE",
      "doorStatus": "CLOSED",
      "timestamp": 1775940368110
    }
  }
}
```

### Transformación en Servicio

```javascript
// Extrae response.data
// Convierte objeto a array
// Transforma cada elevador
const elevators = [
  {
    id: "elev-1",
    number: 1,
    currentFloor: 1,
    destinationFloor: 1,
    status: "IDLE",
    direction: "STOPPED",
    weight: 0,
    doorStatus: "closed"
  },
  {
    id: "elev-2",
    number: 2,
    currentFloor: 1,
    destinationFloor: 1,
    status: "IDLE",
    direction: "STOPPED",
    weight: 0,
    doorStatus: "closed"
  }
]
```

### Frontend Recibe

```javascript
// En App.jsx
setElevators(elevators) // Array transformado ✅

// En ElevatorSelector.jsx
elevators.map(e => (
  // Ahora e.id, e.number, e.currentFloor existen ✅
))
```

---

## 🐛 Qué se Fue Arreglado

1. ❌ API devuelve objeto → ✅ Frontend recibe array
2. ❌ Campos con nombres diferentes → ✅ Nombres consistentes
3. ❌ Direction: "NONE" → ✅ Direction: "STOPPED"
4. ❌ ElevatorSelector.jsx falla con null → ✅ Validación defensiva
5. ❌ ElevatorPanel.jsx falla sin elevator → ✅ Validación defensiva

---

## 🔄 Flujo Actualizado

```
API Backend                    ElevatorService           Frontend Components
     │                                │                         │
     ├─ GET /api/elevators           │                         │
     │         {"data": {...}}       │                         │
     │───────────────────────→        │                         │
     │                               │                         │
     │                         transformElevator()             │
     │                               │                         │
     │                           Extract data                  │
     │                              │                         │
     │                         Object → Array                  │
     │                              │                         │
     │                         Map Fields                      │
     │                              │                         │
     │                         Return Array                    │
     │                               │                         │
     │                               ├──────────→ setElevators()
     │                               │                  │
     │                               │            elevators.map()
     │                               │                  │
     │                               │           ✅ Works!
```

---

## ✅ Verificación

Para verificar que la transformación funciona:

```javascript
// En console del navegador
ElevatorService.getElevators().then(data => {
  console.log('Array?', Array.isArray(data))  // true ✅
  console.log('Primera item:', data[0])       // {id, number, ...} ✅
  console.log('Tiene ID?', data[0]?.id)       // "elev-1" ✅
  console.log('Tiene number?', data[0]?.number) // 1 ✅
})
```

---

**Conclusión**: La transformación es automática y transparente. El frontend siempre recibe datos en el formato correcto, sin importar cómo el backend los envíe.
