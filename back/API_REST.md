# API REST Smart Elevator

## Descripción

API REST asincrónica para gestionar múltiples elevadores con soporte para:
- Control multi-elevador
- Operaciones asincrónicas con `CompletableFuture`
- Server-Sent Events (SSE) para recibir eventos en tiempo real
- Contratos claros y validados con DTO

---

## Configuración

La API está disponible en `http://localhost:8080/api/v1/elevators`

### Características Técnicas

- **Framework**: Spring Boot 3.2.0
- **API**: REST con contenido-type JSON
- **Asincronía**: CompletableFuture + WebFlux
- **Eventos**: Server-Sent Events (SSE)
- **Validación**: Jakarta Validation
- **Serialización**: Jackson JSON

---

## Endpoints de Estado

### 1. Obtener estado de todos los elevadores

```http
GET /api/v1/elevators
```

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Estados de elevadores obtenidos exitosamente",
  "data": {
    "elevator-1": {
      "elevatorId": "elevator-1",
      "currentFloor": 2,
      "targetFloor": 2,
      "state": "IDLE",
      "direction": "NONE",
      "door": {
        "state": "CLOSED"
      },
      "timestamp": 1712884500000
    },
    "elevator-2": {
      "elevatorId": "elevator-2",
      "currentFloor": 1,
      "targetFloor": 1,
      "state": "IDLE",
      "direction": "NONE",
      "door": {
        "state": "CLOSED"
      },
      "timestamp": 1712884500000
    }
  },
  "timestamp": 1712884500123
}
```

### 2. Obtener estado de un elevador específico

```http
GET /api/v1/elevators/{elevatorId}
```

**Ejemplo**:
```bash
curl http://localhost:8080/api/v1/elevators/elevator-1
```

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Estado del elevador obtenido exitosamente",
  "data": {
    "elevatorId": "elevator-1",
    "currentFloor": 2,
    "targetFloor": 2,
    "state": "IDLE",
    "direction": "NONE",
    "door": {
      "state": "CLOSED"
    },
    "timestamp": 1712884500000
  },
  "timestamp": 1712884500123
}
```

### 3. Obtener resumen de elevadores

```http
GET /api/v1/elevators/status/summary
```

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Resumen de elevadores obtenido exitosamente",
  "data": {
    "totalElevators": 2,
    "activeListeners": 3,
    "states": {
      "elevator-1": { ... },
      "elevator-2": { ... }
    }
  },
  "timestamp": 1712884500123
}
```

---

## Endpoints de Control (Asincronía)

### 1. Mover elevador a un piso (ASINCRÓNICO)

```http
POST /api/v1/elevators/{elevatorId}/go-to-floor
Content-Type: application/json

{
  "targetFloor": 4
}
```

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 4}'
```

**Validaciones**:
- `targetFloor` debe estar entre 1 y 5
- La puerta debe estar cerrada
- El elevador no debe estar en estado ERROR

**Respuesta AsincrónicaInitial (202 Accepted o 200 OK)**:
```json
{
  "success": true,
  "message": "Elevador movido al piso 4",
  "data": {
    "elevatorId": "elevator-1",
    "currentFloor": 4,
    "targetFloor": 4,
    "state": "IDLE",
    "direction": "NONE",
    "door": {
      "state": "CLOSED"
    },
    "timestamp": 1712884506000
  },
  "timestamp": 1712884506123
}
```

**Eventos SSE emitidos durante el proceso**:
1. `MOVING` - Elevador inicia movimiento
2. `ARRIVED` - Elevador llega al piso destino

### 2. Abrir puerta (ASINCRÓNICO)

```http
POST /api/v1/elevators/{elevatorId}/door/open
```

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/door/open
```

**Condiciones**:
- El elevador debe estar en estado IDLE

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Puerta abierta exitosamente",
  "data": {
    "elevatorId": "elevator-1",
    "currentFloor": 4,
    "targetFloor": 4,
    "state": "DOOR_OPEN",
    "direction": "NONE",
    "door": {
      "state": "OPEN"
    },
    "timestamp": 1712884510000
  },
  "timestamp": 1712884510123
}
```

**Evento SSE emitido**:
- `DOOR_OPENED` - Puerta abierta

### 3. Cerrar puerta (ASINCRÓNICO)

```http
POST /api/v1/elevators/{elevatorId}/door/close
```

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/door/close
```

**Condiciones**:
- La puerta debe estar en estado OPEN o DOOR_OPEN

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Puerta cerrada exitosamente",
  "data": {
    "elevatorId": "elevator-1",
    "currentFloor": 4,
    "targetFloor": 4,
    "state": "IDLE",
    "direction": "NONE",
    "door": {
      "state": "CLOSED"
    },
    "timestamp": 1712884512000
  },
  "timestamp": 1712884512123
}
```

**Evento SSE emitido**:
- `DOOR_CLOSED` - Puerta cerrada

### 4. Reiniciar elevador (ASINCRÓNICO)

```http
POST /api/v1/elevators/{elevatorId}/reset
```

**Ejemplo con cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/reset
```

**Efecto**:
- Reinicia el elevador al piso 1
- Estado vuelve a IDLE
- Puerta se cierra

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "message": "Elevador reiniciado exitosamente",
  "data": {
    "elevatorId": "elevator-1",
    "currentFloor": 1,
    "targetFloor": 1,
    "state": "IDLE",
    "direction": "NONE",
    "door": {
      "state": "CLOSED"
    },
    "timestamp": 1712884515000
  },
  "timestamp": 1712884515123
}
```

**Evento SSE emitido**:
- `RESET` - Elevador reiniciado

---

## Eventos SSE (Server-Sent Events)

### Suscribirse a eventos de un elevador específico

```http
GET /api/v1/elevators/{elevatorId}/events
```

**Ejemplo con JavaScript**:
```javascript
// Cliente SSE para un elevador específico
const eventSource = new EventSource('/api/v1/elevators/elevator-1/events');

eventSource.addEventListener('MOVING', (event) => {
  const data = JSON.parse(event.data);
  console.log('Elevador en movimiento:', data);
});

eventSource.addEventListener('ARRIVED', (event) => {
  const data = JSON.parse(event.data);
  console.log('Elevador llegó:', data);
});

eventSource.addEventListener('DOOR_OPENED', (event) => {
  const data = JSON.parse(event.data);
  console.log('Puerta abierta:', data);
});

eventSource.addEventListener('DOOR_CLOSED', (event) => {
  const data = JSON.parse(event.data);
  console.log('Puerta cerrada:', data);
});

eventSource.addEventListener('RESET', (event) => {
  const data = JSON.parse(event.data);
  console.log('Elevador reiniciado:', data);
});

eventSource.addEventListener('ERROR', (event) => {
  const data = JSON.parse(event.data);
  console.error('Error en elevador:', data);
});

eventSource.onerror = (event) => {
  console.error('Conexión SSE cerrada', event);
  eventSource.close();
};
```

### Suscribirse a eventos de TODOS los elevadores

```http
GET /api/v1/elevators/events/all
```

**Ejemplo con JavaScript**:
```javascript
// Cliente SSE para TODOS los elevadores
const eventSource = new EventSource('/api/v1/elevators/events/all');

eventSource.addEventListener('MOVING', (event) => {
  const data = JSON.parse(event.data);
  console.log(`Elevador ${data.elevatorId} en movimiento`);
});

eventSource.addEventListener('ARRIVED', (event) => {
  const data = JSON.parse(event.data);
  console.log(`Elevador ${data.elevatorId} llegó al piso ${data.state.currentFloor}`);
});
```

### Formato del evento SSE

```json
{
  "elevatorId": "elevator-1",
  "eventType": "MOVING",
  "state": {
    "elevatorId": "elevator-1",
    "currentFloor": 2,
    "targetFloor": 4,
    "state": "MOVING",
    "direction": "UP",
    "door": {
      "state": "CLOSED"
    },
    "timestamp": 1712884502000
  },
  "message": "Elevador en movimiento al piso 4",
  "timestamp": 1712884502123
}
```

### Tipos de eventos SSE

| Evento | Descripción | Cuándo ocurre |
|--------|-------------|---------------|
| `MOVING` | Elevador inicia movimiento | Al llamar a `/go-to-floor` |
| `ARRIVED` | Elevador llega al destino | Cuando el elevador completó el movimiento |
| `DOOR_OPENED` | Puerta se abre | Al llamar a `/door/open` |
| `DOOR_CLOSED` | Puerta se cierra | Al completar cierre en `/door/close` |
| `RESET` | Elevador reiniciado | Al llamar a `/reset` |
| `ERROR` | Error en operación | Cuando ocurre un error |

---

## Ejemplo Completo de Flujo

### 1. Obtener estado inicial
```bash
curl http://localhost:8080/api/v1/elevators/elevator-1
```

### 2. Suscribirse a eventos (en terminal separada)
```bash
# Con cURL
curl --no-buffer -N http://localhost:8080/api/v1/elevators/elevator-1/events

# O con curl en bash
while true; do
  curl --no-buffer -N http://localhost:8080/api/v1/elevators/elevator-1/events
done
```

### 3. Solicitar movimiento (en otra terminal)
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 5}'
```

### 4. Abrir puerta
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/door/open
```

### 5. Cerrar puerta
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/door/close
```

### 6. Reiniciar
```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/reset
```

---

## Estados del Elevador

| Estado | Descripción |
|--------|-------------|
| `IDLE` | Elevador en reposo, puerta cerrada |
| `MOVING` | Elevador en movimiento |
| `DOOR_OPEN` | Puerta abierta, elevador detenido |
| `DOOR_CLOSING` | Puerta en proceso de cierre |
| `ERROR` | Elevador en estado de error |

---

## Direcciones

| Dirección | Descripción |
|-----------|-------------|
| `UP` | Elevador moviéndose hacia arriba |
| `DOWN` | Elevador moviéndose hacia abajo |
| `NONE` | Elevador no se está moviendo |

---

## Estado de Puertas

| Estado | Descripción |
|--------|-------------|
| `OPEN` | Puerta abierta |
| `CLOSED` | Puerta cerrada |
| `CLOSING` | Puerta cerrándose |

---

## Códigos de Respuesta HTTP

| Código | Descripción |
|--------|-------------|
| `200 OK` | Solicitud exitosa |
| `202 Accepted` | Solicitud aceptada (asincrónica) |
| `400 Bad Request` | Parámetros inválidos |
| `404 Not Found` | Elevador no encontrado |
| `500 Internal Server Error` | Error del servidor |

---

## Manejo de Errores

Cuando ocurre un error, la respuesta es:

```json
{
  "success": false,
  "message": "Descripción del error",
  "data": null,
  "timestamp": 1712884515123
}
```

**Ejemplos de errores**:
- "Piso inválido: 6 (rango válido: 1-5)"
- "No se puede mover: la puerta está abierta"
- "Solo se puede abrir la puerta en estado IDLE"
- "Elevador no encontrado: elevator-3"

---

## Notas de Implementación

### Asincronía

Todos los endpoints de control (`POST`) retornan `CompletableFuture`, lo que significa:
- La solicitud se procesa de forma asincrónica
- La respuesta se envía cuando la operación completa
- Los eventos SSE son emitidos durante el proceso

### Multi-Elevador

- Cada elevador se identifica con un `elevatorId` único (ej: "elevator-1", "elevator-2", etc.)
- Se crean automáticamente al acceder a ellos por primera vez
- Cada elevador mantiene su propios estado independently

### SSE Timeout

- Las conexiones SSE tienen un timeout de 60 segundos
- Si el cliente no recibe eventos durante este tiempo, la conexión se recicla
- El cliente debe reconectar automáticamente

### Limitaciones

- Pisos válidos: 1 a 5
- Solo un movimiento por elevador a la vez
- La puerta debe estar cerrada para mover el elevador
- El elevador debe estar IDLE para abrir la puerta

---

## Testing

### Con curl

```bash
# Terminal 1: Escuchar eventos
curl --no-buffer -N http://localhost:8080/api/v1/elevators/elevator-1/events

# Terminal 2: Enviar comandos
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 3}'
```

### Con navegador

Crear un archivo HTML:
```html
<!DOCTYPE html>
<html>
<head>
    <title>Elevator Control</title>
</head>
<body>
    <h1>Control de Elevadores</h1>
    
    <div>
        <h2>Elevator-1</h2>
        <button onclick="goToFloor('elevator-1', 1)">Piso 1</button>
        <button onclick="goToFloor('elevator-1', 2)">Piso 2</button>
        <button onclick="openDoor('elevator-1')">Abrir</button>
        <button onclick="closeDoor('elevator-1')">Cerrar</button>
        <div id="status-1"></div>
        <div id="events-1"></div>
    </div>

    <script>
        // Conectar a SSE
        const sse = new EventSource('/api/v1/elevators/elevator-1/events');
        
        sse.onmessage = (event) => {
            const data = JSON.parse(event.data);
            document.getElementById('events-1').innerHTML = '<pre>' + JSON.stringify(data, null, 2) + '</pre>';
        };

        async function goToFloor(elevatorId, floor) {
            const response = await fetch(`/api/v1/elevators/${elevatorId}/go-to-floor`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ targetFloor: floor })
            });
            const data = await response.json();
            document.getElementById('status-1').innerHTML = JSON.stringify(data.data, null, 2);
        }

        async function openDoor(elevatorId) {
            const response = await fetch(`/api/v1/elevators/${elevatorId}/door/open`, {
                method: 'POST'
            });
            const data = await response.json();
            document.getElementById('status-1').innerHTML = JSON.stringify(data.data, null, 2);
        }

        async function closeDoor(elevatorId) {
            const response = await fetch(`/api/v1/elevators/${elevatorId}/door/close`, {
                method: 'POST'
            });
            const data = await response.json();
            document.getElementById('status-1').innerHTML = JSON.stringify(data.data, null, 2);
        }
    </script>
</body>
</html>
```

---

## Conclusión

Esta API REST proporciona un control completo multi-elevador con:
- ✅ Contratos claros mediante DTOs validados
- ✅ Operaciones asincrónicas con CompletableFuture
- ✅ Transmisión de eventos en tiempo real con SSE
- ✅ Soporte para múltiples elevadores simultáneos
- ✅ Manejo robusto de errores
