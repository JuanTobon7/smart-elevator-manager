# Arquitectura Hexagonal - Smart Elevator

## Visión General

El proyecto implementa **Arquitectura Hexagonal (Ports & Adapters)** para lograr un diseño que sea:

- **Independiente de frameworks**: La lógica de negocio no depende de Spring
- **Testeable**: Fácil de probar sin dependencias externas
- **Mantenible**: Cambios en hardware o UI no afectan el dominio
- **Escalable**: Fácil agregar nuevos adaptadores

```
┌─────────────────────────────────────────────────────────┐
│                   EXTERNAL WORLD                         │
├─────────────────────────────────────────────────────────┤
│  Hardware   │  Simulador  │  REST API  │  Web UI         │
│  (Arduino)  │  (Thread)   │  (HTTP)    │  (WebSocket)    │
└─────────────────────────────────────────────────────────┘
              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────┐
│           INFRASTRUCTURE LAYER (Adapters)                │
│                                                          │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │ SimulatorAdapter │      │ ArduinoAdapter   │        │
│  │ (implements      │      │ (implements      │        │
│  │ HardwarePort)    │      │ HardwarePort)    │        │
│  └────────┬─────────┘      └────────┬─────────┘        │
│           │                          │                   │
│  ┌────────────────────────────────────────┐             │
│  │    FileEventLogger (EventLogger)       │             │
│  └────────────────────────────────────────┘             │
└──────────────┬───────────────────────┬───────────────────┘
               │ (Ports)               │ (Ports)
┌──────────────▼───────────────────────▼───────────────────┐
│         APPLICATION LAYER (Services)                      │
│                                                          │
│  ┌──────────────────────┐    ┌──────────────────────┐  │
│  │ ElevatorService      │    │ DoorService          │  │
│  │ (implements          │    │ (implements          │  │
│  │  ElevatorUseCase)    │    │  DoorUseCase)        │  │
│  │                      │    │                      │  │
│  │ Uses: HardwarePort   │    │ Uses: HardwarePort   │  │
│  │       EventLogger    │    │       EventLogger    │  │
│  │       Elevator       │    │       Elevator       │  │
│  └──────────────────────┘    └──────────────────────┘  │
└──────────────┬───────────────────────┬───────────────────┘
               │ (UseCases)            │ (UseCases)
┌──────────────▼───────────────────────▼───────────────────┐
│         DOMAIN LAYER (Business Logic)                     │
│                                                          │
│  DOMAIN MODELS:                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Elevator         - Máquina de estados            │  │
│  │ Floor            - Validación de piso            │  │
│  │ Door             - Estado de puerta              │  │
│  │ SensorReading    - Lectura de sensores           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  ENUMS:                                                 │
│  ├─ ElevatorState (IDLE, MOVING, DOOR_OPEN, ...)     │
│  ├─ Direction (UP, DOWN, NONE)                        │
│  └─ DoorState (OPEN, CLOSED, CLOSING)                │
│                                                          │
│  EXCEPTIONS:                                            │
│  └─ ElevatorException - Violaciones de negocio        │
│                                                          │
│  REGLAS IMPLEMENTADAS:                                  │
│  • No mover si puerta está abierta                     │
│  • Solo abrir puerta en IDLE                           │
│  • Validación de piso (1-5)                            │
│  • Máquina de estados completa                         │
└──────────────────────────────────────────────────────────┘
```

## Capas y Responsabilidades

### 1. DOMAIN LAYER (`domain/`)

**Responsabilidad**: Encapsular la lógica de negocio puro, sin depender de frameworks.

- **model/** - Entidades de dominio
  - `Elevator`: Máquina de estados, lógica principal
  - `Floor`: Validación de pisos (1-5)
  - `Door`: Estado de puerta
  - `SensorReading`: DTO de lectura de sensores

- **enums/** - Valores enumerados
  - `ElevatorState`: IDLE, MOVING, DOOR_OPEN, DOOR_CLOSING, ERROR
  - `Direction`: UP, DOWN, NONE
  - `DoorState`: OPEN, CLOSED, CLOSING

- **exception/** - Excepciones de dominio
  - `ElevatorException`: Violaciones de reglas de negocio

**Características**:
- ✅ POJO puro (sin anotaciones Spring)
- ✅ Solo usa Lombok (@Getter, @Setter, @Builder)
- ✅ Totalmente testeable sin Spring
- ✅ Reutilizable en otros contextos

### 2. APPLICATION LAYER (`application/`)

**Responsabilidad**: Coordinación entre dominio e infraestructura mediante puertos.

- **port/in/** - Puertos de entrada (Casos de uso)
  - `ElevatorUseCase`: goToFloor(), readSensors(), reset()
  - `DoorUseCase`: open(), close()

- **port/out/** - Puertos de salida (Interfaces a adapters)
  - `HardwarePort`: Abstracción del hardware
  - `EventLogger`: Abstracción de logging

- **service/** - Servicios que implementan casos de uso
  - `ElevatorService`: Orquesta Elevator + HardwarePort + EventLogger
  - `DoorService`: Maneja puerta con timing simulado

**Características**:
- ✅ Spring beans (@Service)
- ✅ Inyección por constructor (@RequiredArgsConstructor)
- ✅ Implementan interfaces de puertos
- ✅ Coordinan entre dominio e infraestructura

### 3. INFRASTRUCTURE LAYER (`infrastructure/`)

**Responsabilidad**: Implementaciones concretas de puertos (adaptadores).

- **adapter/** - Adaptadores que implementan puertos
  - `SimulatorAdapter` (for SimulatorAdapter)
    - Simula hardware sin Arduino
    - Thread.sleep para delays realistas
    - Perfil: @Profile("simulator")
  
  - `ArduinoAdapter` (for ArduinoAdapter)
    - Stub para comunicación serial
    - TODO: Implementar jSerialComm
    - Perfil: @Profile("arduino")
  
  - `FileEventLogger`
    - Escribe eventos en elevator.log
    - Formato: timestamp | event | details

- **config/** - Configuración de Spring
  - `ElevatorConfig`: Factory para bean Elevator

**Características**:
- ✅ Spring beans (@Component, @Profile)
- ✅ Implementan puertos de salida
- ✅ Intercambiables según perfil

### 4. UI LAYER (`ui/console/`)

**Responsabilidad**: Interfaz de usuario (actualmente consola).

- `ConsoleApp`: CommandLineRunner, loop de comandos
- `CommandParser`: Parse de comandos de usuario

**Características**:
- ✅ Spring bean (@Component)
- ✅ Perfil: @Profile("!test") para no ejecutar en tests
- ✅ Implementa CommandLineRunner

## Puertos (Interfaces)

### Puertos de Entrada (In)

```java
// Define qué puede hacer el elevador desde fuera
public interface ElevatorUseCase {
    void goToFloor(int floor);
    SensorReading readSensors();
    void reset();
}

public interface DoorUseCase {
    void open();
    void close();
}
```

### Puertos de Salida (Out)

```java
// Define cómo se comunica con el mundo externo
public interface HardwarePort {
    void executeCommand(String command);
    SensorReading readState() throws InterruptedException;
    void moveToFloor(int floor);
    void openDoor();
    void closeDoor();
    void reset();
}

public interface EventLogger {
    void logEvent(String eventType, String details, SensorReading reading);
    void logError(String errorMessage);
}
```

## Flujo de una Operación Completa

### Ejemplo: GO 3

```
┌─────────────────────────────────────────────────────────┐
│ 1. Usuario escribe "GO 3"                               │
└────────────────┬────────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. CommandParser.parseAndExecute()                      │
│    - Parsea comando                                      │
│    - Llama elevatorUseCase.goToFloor(3)                 │
└────────────────┬────────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. ElevatorService.goToFloor(3) (Application)           │
│    a. Delega a dominio: elevator.goToFloor(3)           │
│       - Valida: puerta cerrada, piso válido            │
│       - Transición: IDLE → MOVING                      │
│    b. Ejecuta en adapter: hardwarePort.moveToFloor(3)  │
│    c. Simula movimiento: Thread.sleep(500 * 2)         │
│    d. Notifica llegada: elevator.arriveAtFloor()       │
│       - Transición: MOVING → IDLE                      │
│    e. Log: eventLogger.logEvent(\"ARRIVED\", ...)      │
└────────────────┬────────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. SimulatorAdapter.moveToFloor(3)                      │
│    - Inicia movimiento simulado                         │
│    - Espera tiempo proporcional (500ms * pisos)         │
│    - Actualiza estado interno                           │
└────────────────┬────────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. FileEventLogger.logEvent(...)                        │
│    - Escribe en elevator.log                            │
│    - Formato: timestamp | eventType | details          │
└────────────────┬────────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Output en consola:                                  │
│    [OK] IDLE | Floor: 3 | Door: CLOSED                 │
└─────────────────────────────────────────────────────────┘
```

## Decisiones de Diseño

### 1. Domain Layer sin Spring
**Razón**: La lógica de negocio debe ser independiente del framework
- Facilita testing único sin Spring
- Reutilizable en otros contextos
- Enfoque Domain-Driven Design

### 2. Puertos para Hardware
**Razón**: Permitir intercambiar SimulatorAdapter por ArduinoAdapter
- SimulatorAdapter con Thread.sleep
- ArduinoAdapter actual es stub (TODO list)
- Fácil cambiar sin tocar dominio/services

### 3. Spring Profiles
- **simulator** (default): Para desarrollo y testing
- **arduino**: Para hardware real (futuro)
- **!test**: ConsoleApp no ejecuta durante tests

### 4. Logging con SLF4J manual
**Razón**: Evitar problemas de procesamiento de anotaciones Lombok
- @Slf4j requiere processor de anotaciones
- SLF4J manual es más portable

## Extensibilidad

### Agregar nuevo adaptador (e.g., MockAdapter)

1. Crear clase que implemente `HardwarePort`
2. Agregar `@Component @Profile("mock")`
3. Implementar todos los métodos
4. Ejecutar con `--spring.profiles.active=mock`

Ejemplo:
```java
@Component
@Profile("mock")
public class MockHardwareAdapter implements HardwarePort {
    // Implementación de pruebas
}
```

### Agregar nuevo caso de uso

1. Crear interfaz en `application/port/in/`
2. Crear servicio en `application/service/` que implemente la interfaz
3. Inyectar en ConsoleApp si es necesario

## Testing

### Unit Tests (Sin Spring)
- `ElevatorTest`: Prueba lógica de dominio pura
- Enfoque: máquina de estados, validaciones
- Ejecución: `mvn test -Dtest=ElevatorTest`

### Integration Tests (Con Spring)
- `ElevatorServiceTest`: Prueba servicios + adapters
- Perfil: @ActiveProfiles("simulator")
- Enfoque: operaciones end-to-end

## Matriz de Componentes

| Componente | Layer | Spring? | Testeable Solo? | Intercambiable? |
|---|---|---|---|---|
| Elevator | Domain | No | ✅ | N/A |
| ElevatorService | Application | Yes | ❌ | ❌ |
| SimulatorAdapter | Infrastructure | Yes | ❌ | ✅ |
| FileEventLogger | Infrastructure | Yes | ❌ | ✅ |
| ConsoleApp | UI | Yes | ❌ | ✅ |

## Referencias

- Domain-Driven Design (Eric Evans)
- Hexagonal Architecture (Alistair Cockburn)
- Clean Architecture (Robert C. Martin)
