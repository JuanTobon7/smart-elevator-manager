# Smart Elevator - Proyecto Arquitectura de Software Avanzada

## 📋 Descripción General

Sistema de control inteligente de elevadores implementado con **Arquitectura Hexagonal** en Spring Boot 3.2.0, incluye:

- **API REST asincrónica** con contratos claros (DTOs)
- **Server-Sent Events (SSE)** para transmisión en tiempo real
- **Multi-elevador** con gestión independiente
- **Máquina de estados** implementada en el dominio
- **Completamente desacoplado** del framework Spring

---

## 🎯 Características Principales

✅ **API REST RESTful** - Endpoints bien documentados
✅ **Asincronía** - CompletableFuture para operaciones no bloqueantes
✅ **SSE (Server-Sent Events)** - Eventos en tiempo real
✅ **Multi-elevador** - Manejo de múltiples elevadores simultáneamente
✅ **Contratos claros** - DTOs validados con Jakarta Validation
✅ **Arquitectura Hexagonal** - Dominio desacoplado
✅ **Máquina de Estados** - Control del flujo del elevador
✅ **Logging detallado** - Trazabilidad completa
✅ **CORS habilitado** - Para consumo desde frontend
✅ **Cliente Web** - Panel de control interactivo

---

## 📁 Estructura del Proyecto

```
ELEVATOR_2026/
├── src/
│   ├── main/
│   │   ├── java/co/edu/unillanos/elevator/
│   │   │   ├── ElevatorApplication.java              # Main Spring Boot
│   │   │   ├── application/
│   │   │   │   ├── port/in/
│   │   │   │   │   ├── DoorUseCase.java
│   │   │   │   │   └── ElevatorUseCase.java
│   │   │   │   └── service/
│   │   │   │       ├── DoorService.java
│   │   │   │       └── ElevatorService.java
│   │   │   ├── domain/
│   │   │   │   ├── enums/
│   │   │   │   │   ├── Direction.java
│   │   │   │   │   ├── DoorState.java
│   │   │   │   │   └── ElevatorState.java
│   │   │   │   ├── exception/
│   │   │   │   │   └── ElevatorException.java
│   │   │   │   └── model/
│   │   │   │       ├── Door.java
│   │   │   │       ├── Elevator.java
│   │   │   │       ├── Floor.java
│   │   │   │       └── SensorReading.java
│   │   │   ├── infrastructure/
│   │   │   │   ├── adapter/
│   │   │   │   │   ├── ArduinoAdapter.java
│   │   │   │   │   ├── FileEventLogger.java
│   │   │   │   │   └── SimulatorAdapter.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── ElevatorConfig.java
│   │   │   │   │   └── SpringElevatorConfig.java      # ✨ NUEVO
│   │   │   │   ├── event/                             # ✨ NUEVO
│   │   │   │   │   ├── ElevatorEventBroadcaster.java
│   │   │   │   │   ├── ElevatorEventListener.java
│   │   │   │   │   └── SseElevatorEventListener.java
│   │   │   │   └── elevator/                          # ✨ NUEVO
│   │   │   │       ├── ElevatorManager.java
│   │   │   │       └── ElevatorAsyncService.java
│   │   │   └── ui/
│   │   │       ├── console/
│   │   │       │   ├── CommandParser.java
│   │   │       │   └── ConsoleApp.java
│   │   │       └── rest/                              # ✨ NUEVO
│   │   │           ├── controller/
│   │   │           │   └── ElevatorRestController.java
│   │   │           └── dto/
│   │   │               ├── ApiResponseDTO.java
│   │   │               ├── DoorActionRequestDTO.java
│   │   │               ├── DoorDTO.java
│   │   │               ├── ElevatorEventDTO.java
│   │   │               ├── ElevatorStateDTO.java
│   │   │               └── GoToFloorRequestDTO.java
│   │   └── resources/
│   │       └── application.properties                 # ✨ ACTUALIZADO
│   └── test/
│       └── java/co/edu/unillanos/elevator/
│           ├── application/
│           │   └── ElevatorServiceTest.java
│           └── domain/
│               └── ElevatorTest.java
├── pom.xml                                             # ✨ ACTUALIZADO
├── API_REST.md                                         # ✨ NUEVO
├── client.html                                         # ✨ NUEVO
├── QUICK_START.md
├── ARCHITECTURE.md
├── DEVELOPMENT.md
└── README.md (este archivo)
```

---

## 🚀 Inicio Rápido

### Requisitos

- Java 17+
- Maven 3.8.9+
- Spring Boot 3.2.0

### 1. Clonar y compilar

```bash
cd ELEVATOR_2026
mvn clean install
```

### 2. Ejecutar la aplicación

```bash
# Opción 1: Con Maven
mvn spring-boot:run

# Opción 2: Con el script
./run.bat (Windows)
./run.sh  (Linux/Mac)
```

La aplicación estará disponible en `http://localhost:8080`

### 3. Acceder al panel de control

Abre en tu navegador:
```
http://localhost:8080/client.html
```

---

## 📡 API REST - Guía Rápida

### Ejemplo: Obtener estado de todos los elevadores

```bash
curl http://localhost:8080/api/v1/elevators
```

### Ejemplo: Mover elevador al piso 3

```bash
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 3}'
```

### Ejemplo: Suscribirse a eventos

```bash
curl --no-buffer -N http://localhost:8080/api/v1/elevators/elevator-1/events
```

**Documentación completa:** Ver [API_REST.md](API_REST.md)

---

## 🎮 Endpoints Disponibles

### Estados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/elevators` | Obtener estado de todos los elevadores |
| GET | `/api/v1/elevators/{elevatorId}` | Obtener estado de un elevador |
| GET | `/api/v1/elevators/status/summary` | Resumen general |

### Control (Asincrónico)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/elevators/{elevatorId}/go-to-floor` | Mover a piso |
| POST | `/api/v1/elevators/{elevatorId}/door/open` | Abrir puerta |
| POST | `/api/v1/elevators/{elevatorId}/door/close` | Cerrar puerta |
| POST | `/api/v1/elevators/{elevatorId}/reset` | Reiniciar elevador |

### Eventos SSE

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/elevators/{elevatorId}/events` | Eventos de elevador específico |
| GET | `/api/v1/elevators/events/all` | Eventos de todos los elevadores |

---

## 🔧 Configuración

Archivo: `src/main/resources/application.properties`

```properties
# Puerto
server.port=8080

# SSE
spring.mvc.async.request-timeout=60000

# Logging
logging.level.co.edu.unillanos=DEBUG
```

---

## 📊 Máquina de Estados del Elevador

```
┌─────────┐
│  IDLE   │ ◄──────────────┐
└────┬────┘               │
     │ goToFloor()        │
     ▼                     │
  MOVING  ────────────► IDLE (arriveAtFloor)
     │
     │ openDoor()
     ▼
 DOOR_OPEN
     │
     │ closeDoor()
     ▼
DOOR_CLOSING ──┐
                │ (completeDoorClosing)
                └──► IDLE
    
    ERROR ◄── Error en cualquier estado
     │
     └──► IDLE (reset)
```

---

## 🧪 Testing

### Prueba con cURL

```bash
# Terminal 1: Escuchar eventos
curl --no-buffer http://localhost:8080/api/v1/elevators/elevator-1/events

# Terminal 2: Enviar comandos
curl -X POST http://localhost:8080/api/v1/elevators/elevator-1/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 5}'
```

### Prueba de Multi-elevador

```bash
# Crear y controlar elevator-2
curl -X POST http://localhost:8080/api/v1/elevators/elevator-2/go-to-floor \
  -H "Content-Type: application/json" \
  -d '{"targetFloor": 2}'
```

### Con JavaScript

Ver `client.html` para ejemplo completo con EventSource y API Fetch

---

## 🏗️ Arquitectura

### Capas

1. **Domain** (Independiente del framework)
   - Entidades: `Elevator`, `Door`, `Floor`
   - Enums: `ElevatorState`, `Direction`, `DoorState`
   - Excepciones: `ElevatorException`
   - Lógica pura de negocio

2. **Application** (Casos de uso)
   - `ElevatorService`, `DoorService`
   - Puertos: `ElevatorUseCase`, `DoorUseCase`
   - Orquestación de operaciones

3. **Infrastructure** (Spring Boot)
   - `ElevatorManager` - Gestión multi-elevador
   - `ElevatorAsyncService` - Operaciones asincrónicas
   - `ElevatorEventBroadcaster` - Eventos SSE
   - Adaptadores: `ArduinoAdapter`, `SimulatorAdapter`

4. **UI** (Presentación)
   - `ElevatorRestController` - API REST
   - DTOs para contratos claros
   - `client.html` - Panel web

---

## 💡 Conceptos Implementados

### Arquitectura Hexagonal

- Dominio desacoplado totalmente de Spring
- Puertos bien definidos
- Adaptadores intercambiables

### Máquina de Estados

- Estados finitos y transiciones claras
- Validación de reglas de negocio
- Prevención de estados inválidos

### Asincronía

- `CompletableFuture` para operaciones async
- No bloqueante
- Manejo de errores con `exceptionally`

### SSE (Server-Sent Events)

- Conexión HTTP unidireccional (servidor → cliente)
- Eventos tipados (MOVING, ARRIVED, etc.)
- Reconexión automática

### Multi-elevador

- `ElevatorManager` - Gestor centralizado
- Cada elevador es independiente
- Fácilmente escalable

---

## 📈 Rendimiento

- **Max Conexiones SSE**: 200 threads configurados
- **Timeout SSE**: 60 segundos
- **Tiempo de movimiento**: 1000ms por piso
- **Buffer máximo**: 1MB

---

## 🐛 Logs y Debugging

### Ver logs en DEBUG

```bash
# Modifica application.properties
logging.level.co.edu.unillanos=DEBUG
```

### Formato de logs

```
2024-04-11 14:30:45 - co.edu.unillanos.elevator.ui.rest.controller.ElevatorRestController - [elevator-1] Solicitud: Mover elevador elevator-1 al piso 3
```

---

## 📚 Documentación Adicional

- [API_REST.md](API_REST.md) - Documentación completa de endpoints
- [ARCHITECTURE.md](ARCHITECTURE.md) - Detalles de arquitectura
- [DEVELOPMENT.md](DEVELOPMENT.md) - Guía de desarrollo
- [QUICK_START.md](QUICK_START.md) - Inicio rápido

---

## 🔐 Seguridad

- CORS habilitado (ajustar en producción)
- Validación de entrada con Jakarta Validation
- Manejo de excepciones centralizado
- Logging de operaciones críticas

---

## 📦 Dependencias Principales

```xml
<!-- Spring Boot -->
spring-boot-starter (3.2.0)
spring-boot-starter-web
spring-boot-starter-webflux

<!-- Validación -->
jakarta.validation-api

<!-- Serialización -->
jackson-databind

<!-- Utilidades -->
lombok
slf4j-api

<!-- Hardware (Optional) -->
jSerialComm (2.10.4)
```

---

## 🤝 Contribuciones

Este proyecto es parte del curso "Arquitectura de Software Avanzada" de la Universidad de los Llanos.

---

## 📝 Licencia

Proyecto académico - Universidad de los Llanos

---

## ✨ Nuevas Características (Última Actualización)

### [2024-04-11] API REST v1.0

- ✨ Controlador REST con CRUD para elevadores
- ✨ DTOs para contratos claros y validados
- ✨ Operaciones asincrónicas con CompletableFuture
- ✨ Server-Sent Events (SSE) para eventos en tiempo real
- ✨ Soporte para múltiples elevadores simultáneamente
- ✨ Panel de control web interactivo
- ✨ Documentación completa de API

---

## 📞 Soporte

Para reportar issues o hacer preguntas, contacta al equipo de desarrollo.

---

**Happy Elevating! 🛗🚀**
