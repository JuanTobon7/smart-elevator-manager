# Implementación de Patrones de Diseño
## Smart Elevator Manager

---

## 1. Patrón Adapter (Estructural)

### Propósito
Permite que el sistema se comunique con hardware incompatible (Arduino real y simulado) a través de la misma interfaz, sin que los servicios de aplicación conozcan con cuál están trabajando.

### Archivos Involucrados

#### 🎯 Target (Interfaz)
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/application/port/out/HardwarePort.java`
- **Descripción**: Define el contrato que todos los adaptadores de hardware deben cumplir
- **Métodos principales**: 
  - `moveToFloor(int floor)`
  - `openDoor()`
  - `closeDoor()`
  - `readState()`
  - `reset()`
  - `executeCommand(String command)`

#### 🔌 Adapter 1: Simulador
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/adapter/SimulatorAdapter.java`
- **Anotación**: `@Profile("simulator")`
- **Descripción**: Implementa `HardwarePort` simulando el hardware en memoria con `Thread.sleep` para tiempos realistas
- **Usado cuando**: `spring.profiles.active=simulator`

#### 🔌 Adapter 2: Arduino Real
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/adapter/ArduinoAdapter.java`
- **Anotación**: `@Profile("arduino")`
- **Descripción**: Implementa `HardwarePort` para comunicación con hardware Arduino real
- **Dependencia**: `SerialPortManager.java` (gestión de puerto serie)
- **Configuración**: Puerto configurable via `arduino.port` en `application.properties`
- **Características**:
  - Inicialización con `@PostConstruct`
  - Limpieza con `@PreDestroy`
  - Validación de conexión con comando PING
  - Polling para esperar llegada a piso

#### 🔌 Adapter 3: Event Logger
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/adapter/FileEventLogger.java`
- **Descripción**: Adaptador de salida que implementa `EventLogger`
- **Funcionalidad**: Escribe eventos del elevador en archivo `elevator.log`

#### 🔧 Mecanismo de Inyección
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/config/ElevatorConfig.java`
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/config/SpringElevatorConfig.java`
- **Descripción**: Spring Boot inyecta el adaptador correcto según el perfil activo
- **Servicios que usan**: 
  - `ElevatorService.java` (solo conoce `HardwarePort`)
  - `DoorService.java` (solo conoce `HardwarePort`)

#### 📝 Evidencia en el Código
```java
// En ElevatorService.java
public void goToFloor(int floor) {
    // Llama hardwarePort sin saber si es simulador o Arduino
    hardwarePort.moveToFloor(floor);
}
```

---

## 2. Patrón Observer (Comportamental)

### Propósito
Múltiples clientes web conectados vía SSE reciben notificaciones automáticas cuando el elevador cambia de estado, sin que el emisor dependa de los receptores.

### Archivos Involucrados

#### 👁️ Observer (Interfaz)
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/event/ElevatorEventListener.java`
- **Descripción**: Define el contrato para observadores de eventos
- **Métodos**:
  - `onEvent(ElevatorEventDTO event)` - recibe el evento
  - `isSubscribedTo(String elevatorId)` - filtra por ID del elevador

#### 📢 Subject (Observable)
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/event/ElevatorEventBroadcaster.java`
- **Descripción**: Mantiene lista thread-safe de observadores
- **Estructura interna**: `CopyOnWriteArrayList<ElevatorEventListener>` (para multihilo)
- **Métodos principales**:
  - `subscribe(ElevatorEventListener listener)` - añade observador
  - `unsubscribe(ElevatorEventListener listener)` - remueve observador
  - `broadcast(ElevatorEventDTO event)` - notifica a todos
  - `broadcastToElevator(String elevatorId, ElevatorEventDTO event)` - notifica listeners filtrados
- **Comentario en el código**: "Broadcaster de eventos de elevadores usando observador pattern"

#### 👂 ConcreteObserver
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/event/SseElevatorEventListener.java`
- **Descripción**: Implementa `ElevatorEventListener` para enviar eventos via SSE
- **Características**:
  - Recibe evento del broadcaster
  - Lo reenvía al cliente web mediante `SseEmitter`
  - Se auto-desuscribe si el cliente se desconecta (`isActive = false`)
  - Manejo de excepciones para clientes desconectados

#### 🎬 Flujo Completo
1. **Disparador**: `ElevatorAsyncService.goToFloorAsync()`
2. **Broadcast**: Llama `eventBroadcaster.broadcastToElevator(elevatorId, evento)`
3. **Filtrado**: Filtra listeners que tengan `isSubscribedTo(elevatorId) = true`
4. **Notificación**: Llama `onEvent(event)` en cada `SseElevatorEventListener` activo

#### 📡 Endpoint SSE
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/controller/ElevatorRestController.java`
- **Método**: Endpoint `/api/elevators/{id}/subscribe` que devuelve `SseEmitter`
- **Descripción**: Los clientes se conectan aquí para recibir eventos en tiempo real

#### DTO de Eventos
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/infrastructure/dto/ElevatorEventDTO.java`
- **Descripción**: Datos que se envían en cada notificación a los clientes

---

## 3. Patrón State (Comportamental)

### Propósito
El elevador tiene comportamientos distintos y operaciones válidas diferentes según su estado actual, evitando dispersar reglas de transición en condicionales por todo el código.

### Archivos Involucrados

#### 📊 Estados Definidos
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/domain/enums/ElevatorState.java`
- **Estados**: `IDLE`, `MOVING`, `DOOR_OPEN`, `DOOR_CLOSING`, `ERROR`
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/domain/enums/DoorState.java`
- **Estados**: `OPEN`, `CLOSED`, `CLOSING`
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/domain/enums/Direction.java`
- **Estados**: `UP`, `DOWN`, `NONE`

#### 🎮 Contexto (State Machine)
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/domain/model/Elevator.java`
- **Descripción**: Contiene el estado actual y expone métodos que cambian de estado
- **Punto único de transiciones**: Es el único lugar donde ocurren cambios de estado
- **Métodos principales**:
  - `goToFloor(int floor)` - transición `IDLE → MOVING`
  - `arriveAtFloor()` - transición `MOVING → IDLE`
  - `openDoor()` - transición `IDLE → DOOR_OPEN`
  - `closeDoor()` - transición `DOOR_OPEN → DOOR_CLOSING`
  - `completeDoorClosing()` - transición `DOOR_CLOSING → IDLE`
  - `setError()` - transición a `ERROR`
  - `reset()` - salida de estado `ERROR`

#### 🚪 Contexto Puerta
- **Archivo**: `src/main/java/co/edu/unillanos/elevator/domain/model/Door.java`
- **Descripción**: Máquina de estados para la puerta del elevador
- **Estados y transiciones**: Similar a la del elevador

#### ✅ Validaciones de Transiciones

| Transición | Método | Validación |
|-----------|--------|-----------|
| IDLE → MOVING | `goToFloor()` | Puerta cerrada + estado ≠ ERROR |
| MOVING → IDLE | `arriveAtFloor()` | Estado == MOVING |
| IDLE → DOOR_OPEN | `openDoor()` | Estado == IDLE (solo parado) |
| DOOR_OPEN → DOOR_CLOSING → IDLE | `closeDoor()`, `completeDoorClosing()` | Validación de estado previo |
| Cualquier → ERROR | `setError()` | Operación inválida |
| ERROR → IDLE | `reset()` | Solo método para salir de ERROR |

#### 📋 Manejo de Errores
- **Excepción**: `src/main/java/co/edu/unillanos/elevator/domain/exception/ElevatorException.java`
- **Uso**: Lanzada cuando se intenta una operación inválida en el estado actual
- **Ejemplo**: Intentar abrir puerta mientras se mueve lanza `ElevatorException`

---

## 4. Conexión con SOLID y GRASP

### SOLID

#### 🔹 SRP (Single Responsibility)
- `ElevatorEventBroadcaster` solo gestiona observadores
- `FileEventLogger` solo escribe en archivo
- `Elevator` solo contiene reglas de dominio
- Cada clase tiene una única razón para cambiar

#### 🔹 OCP (Open/Closed)
- Agregar nuevo adaptador de hardware → NO modifica `HardwarePort` ni `ElevatorService`
- Agregar nuevo observer → NO modifica `ElevatorEventBroadcaster`
- Extensión sin modificación de clases existentes

#### 🔹 LSP (Liskov Substitution)
- `SimulatorAdapter` y `ArduinoAdapter` son intercambiables via `HardwarePort`
- Sistema funciona idéntico con cualquiera de los dos

#### 🔹 DIP (Dependency Inversion)
- `ElevatorService` depende de `HardwarePort` (interfaz)
- `DoorService` depende de `HardwarePort` (interfaz)
- NO dependen de `SimulatorAdapter` o `ArduinoAdapter` (implementaciones)

### GRASP

#### 🎯 Low Coupling
- Capa de dominio (`Elevator`, `Door`, `Floor`) no conoce Spring, SSE, sistema de archivos
- `ElevatorEventBroadcaster` no conoce `SseElevatorEventListener` directamente, solo `ElevatorEventListener`

#### 🎯 High Cohesion
- `ElevatorEventBroadcaster` agrupa solo lógica de gestión de observadores
- `Elevator` agrupa solo reglas y transiciones de máquina de estados

#### 🎯 Protected Variations
- `HardwarePort` protege al sistema de cambios en hardware
- `ElevatorEventListener` protege broadcaster de cambios en cómo clientes consumen eventos

#### 🎯 Controller
- `ElevatorRestController` recibe peticiones HTTP y delega sin ejecutar lógica de negocio
- `ElevatorAsyncService` coordina operaciones del elevador y emisión de eventos

---

## 5. Estructura de Directorios

```
src/main/java/co/edu/unillanos/elevator/
│
├── domain/                          # Lógica de negocio (Estados, Excepciones, Modelos)
│   ├── enums/
│   │   ├── ElevatorState.java      # Estados del elevador
│   │   ├── DoorState.java          # Estados de la puerta
│   │   └── Direction.java          # Direcciones de movimiento
│   ├── exception/
│   │   └── ElevatorException.java   # Excepciones del dominio
│   └── model/
│       ├── Elevator.java            # ✅ STATE PATTERN (máquina de estados)
│       ├── Door.java                # Estados de la puerta
│       ├── Floor.java
│       └── SensorReading.java
│
├── application/                     # Puertos (interfaces de negocio)
│   ├── port/
│   │   ├── in/
│   │   │   ├── ElevatorUseCase.java
│   │   │   └── DoorUseCase.java
│   │   └── out/
│   │       └── HardwarePort.java   # ✅ ADAPTER TARGET
│   └── service/
│       ├── ElevatorService.java    # Usa HardwarePort
│       └── DoorService.java        # Usa HardwarePort
│
└── infrastructure/                  # Implementaciones (Adaptadores, Controladores, DTOs)
    ├── adapter/
    │   ├── SimulatorAdapter.java   # ✅ ADAPTER CONCRETO 1
    │   ├── ArduinoAdapter.java     # ✅ ADAPTER CONCRETO 2
    │   ├── FileEventLogger.java    # ✅ ADAPTER CONCRETO 3
    │   └── SerialPortManager.java  # Gestión puerto serie
    │
    ├── config/
    │   ├── ElevatorConfig.java
    │   └── SpringElevatorConfig.java
    │
    ├── controller/
    │   └── ElevatorRestController.java  # Endpoints REST
    │
    ├── event/
    │   ├── ElevatorEventListener.java      # ✅ OBSERVER (interfaz)
    │   ├── ElevatorEventBroadcaster.java   # ✅ SUBJECT (observable)
    │   └── SseElevatorEventListener.java   # ✅ CONCRETE OBSERVER
    │
    ├── dto/
    │   ├── ElevatorEventDTO.java
    │   ├── ElevatorStateDTO.java
    │   ├── RequestFloorDTO.java
    │   └── GenericResponseDTO.java
    │
    ├── elevator/
    │   ├── ElevatorManager.java
    │   ├── ElevatorOrchestrator.java
    │   └── ElevatorOrchestratorFactoryImpl.java
    │
    └── factory/
        └── ElevatorOrchestratorFactory.java
```

---

## 6. Resumen de Implementación

| Patrón | Problema Resuelto | Clases Clave | Estado |
|--------|-------------------|--------------|--------|
| **Adapter** | Hardware incompatible (Arduino real vs simulado) | `HardwarePort`, `SimulatorAdapter`, `ArduinoAdapter` | ✅ Implementado |
| **Observer** | Notificaciones en tiempo real a múltiples clientes SSE | `ElevatorEventListener`, `ElevatorEventBroadcaster`, `SseElevatorEventListener` | ✅ Implementado |
| **State** | Máquina de estados del elevador con transiciones validadas | `ElevatorState`, `Elevator`, `Door`, `ElevatorException` | ✅ Implementado |

---

## 7. Cómo Estos Patrones Mejoran el Diseño

### Sin Patrones ❌
- **Adapter**: `ElevatorService` tendría `if arduino / if simulator` por todo el código
- **Observer**: `ElevatorAsyncService` conocería cada cliente SSE y los llamaría directamente
- **State**: Reglas de transición dispersas en múltiples condicionales sin control centralizado

### Con Patrones ✅
- **Adapter**: Cambiar de simulador a Arduino es solo cambiar `spring.profiles.active` en `application.properties`
- **Observer**: Agregar nuevo tipo de observador (consola, BD, email) sin modificar clases existentes
- **State**: Todas las reglas centralizadas en `Elevator.java`, imposible ejecutar operación inválida

---

**Proyecto**: Smart Elevator Manager  
**Institución**: Universidad de los Llanos  
**Programa**: Ingeniería de Sistemas  
**Semestre**: 2026-I
