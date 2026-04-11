# Smart Elevator - Sistema de Control Inteligente

Proyecto universitario: Control de elevador inteligente con Arduino, implementado con arquitectura Hexagonal (Ports & Adapters) en Java 17+ y Spring Boot 3.x.

## Características

- **Arquitectura Hexagonal**: Separación clara entre dominio, puertos, adaptadores e infraestructura
- **Domain-Driven Design**: Lógica de negocio independiente de Spring (POJO puro)
- **Máquina de Estados**: Estados bien definidos (IDLE, MOVING, DOOR_OPEN, DOOR_CLOSING, ERROR)
- **Dos Modos de Operación**:
  - **Simulator** (default): Simula el hardware sin Arduino
  - **Arduino**: Stub preparado para comunicación serial con jSerialComm (por implementar)
- **Logging de Eventos**: Registro en archivo `elevator.log`
- **Tests Unitarios e Integración**: JUnit 5 + Spring Boot Test
- **Interfaz de Consola**: Completamente interactiva

## Estructura del Proyecto

```
elevator/
├── src/main/java/co/edu/unillanos/elevator/
│   ├── domain/
│   │   ├── model/       # Entidades de dominio (Elevator, Floor, Door, SensorReading)
│   │   ├── enums/       # ElevatorState, Direction, DoorState
│   │   └── exception/   # ElevatorException
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/      # ElevatorUseCase, DoorUseCase
│   │   │   └── out/     # HardwarePort, EventLogger
│   │   └── service/     # ElevatorService, DoorService
│   ├── infrastructure/
│   │   ├── adapter/     # SimulatorAdapter, ArduinoAdapter, FileEventLogger
│   │   └── config/      # ElevatorConfig
│   ├── ui/
│   │   └── console/     # ConsoleApp, CommandParser
│   └── ElevatorApplication.java
├── src/test/java/...
│   ├── domain/ElevatorTest.java
│   └── application/ElevatorServiceTest.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

## Requisitos

- Java 17 o superior
- Maven 3.6+

## Instalación

1. **Compilar el proyecto**:
```bash
mvn clean install
```

2. **Ejecutar tests**:
```bash
mvn test
```

3. **Ejecutar la aplicación**:
```bash
mvn spring-boot:run
```

O compilar a JAR:
```bash
mvn clean package
java -jar target/smart-elevator-1.0.0.jar
```

## Modos de Operación

### Modo Simulator (por defecto)

```bash
java -jar smart-elevator-1.0.0.jar --spring.profiles.active=simulator
```

O simplemente:
```bash
mvn spring-boot:run
```

### Modo Arduino (stub)

```bash
java -jar smart-elevator-1.0.0.jar --spring.profiles.active=arduino
```

Nota: Actualmente lanza `UnsupportedOperationException`. Ver [ArduinoAdapter.java](src/main/java/co/edu/unillanos/elevator/infrastructure/adapter/ArduinoAdapter.java) para TODOs.

## Comandos de Consola

Una vez iniciada la aplicación, disponibles los siguientes comandos:

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `GO <n>` | Mover a piso n (1-5) | `GO 3` |
| `DOOR OPEN` | Abrir puerta | `DOOR OPEN` |
| `DOOR CLOSE` | Cerrar puerta | `DOOR CLOSE` |
| `READ` | Leer estado actual | `READ` |
| `RESET` | Resetear a estado inicial | `RESET` |
| `EXIT` | Salir del programa | `EXIT` |

### Ejemplo de Sesión

```
> GO 3
[OK] MOVING  | Floor: 3 | Door: CLOSED
[OK] IDLE    | Floor: 3 | Door: CLOSED

> DOOR OPEN
[OK] DOOR_OPEN | Floor: 3 | Door: OPEN

> DOOR CLOSE
[OK] IDLE | Floor: 3 | Door: CLOSED

> READ
[OK] IDLE | Floor: 3 | Door: CLOSED

> RESET
[OK] IDLE | Floor: 1 | Door: CLOSED

> EXIT
Elevador finalizado.
```

## Logging

Los eventos se registran en `elevator.log` con este formato:

```
2026-04-11T10:32:01 | GO       | floor=3 | state=MOVING | door=CLOSED | floor=3
2026-04-11T10:32:03 | ARRIVED  | floor=3 | state=IDLE   | door=CLOSED | floor=3
2026-04-11T10:32:05 | OPEN     | floor=3 | state=DOOR_OPEN | door=OPEN | floor=3
```

## Máquina de Estados

```
    IDLE
    ├─ goToFloor(n) → MOVING
    ├─ openDoor()  → DOOR_OPEN
    └─ reset()     → IDLE
    
    MOVING
    └─ arriveAtFloor() → IDLE
    
    DOOR_OPEN
    ├─ closeDoor() → DOOR_CLOSING
    └─ openDoor()  → IDLE
    
    DOOR_CLOSING
    └─ completeDoorClosing() → IDLE
    
    ERROR (en cualquier transición inválida)
    └─ reset() → IDLE
```

## Reglas de Negocio (Domain Layer)

1. ✅ No se puede mover si la puerta está abierta
2. ✅ Solo se puede abrir puerta en estado IDLE
3. ✅ Solo se pueden cerrar puerta en estado DOOR_OPEN
4. ✅ Solo se puede completar cierre en estado DOOR_CLOSING
5. ✅ Pisos válidos: 1-5
6. ✅ Desde ERROR solo se puede hacer reset()

## Tests

### Tests Unitarios (sin Spring)

```bash
mvn test -Dtest=ElevatorTest
```

Cubre:
- Estados iniciales
- Transiciones de estado válidas
- Validaciones de reglas de negocio
- Excepciones correctas

### Tests de Integración (con Spring)

```bash
mvn test -Dtest=ElevatorServiceTest
```

Cubre:
- Operación completa con SimulatorAdapter
- Secuencias de comandos
- Logging de eventos

## Dependencias

- **Spring Boot 3.2.0**: Framework web/core
- **Lombok 1.18.30**: Reducción de boilerplate
- **jSerialComm 2.10.4**: Comunicación serial (para futuro ArduinoAdapter)
- **JUnit 5**: Testing

## Extensiones Futuras

1. **Implementar ArduinoAdapter**: 
   - Usar jSerialComm para comunicación serial
   - Definir protocolo con firmware Arduino

2. **Agregar REST API**:
   - Spring Boot Web (spring-boot-starter-web)
   - Endpoints para control y monitoreo

3. **UI Web**:
   - Dashboard con estado en tiempo real
   - Simulador visual de pisos

4. **Base de Datos**:
   - Historial de eventos
   - Estadísticas de uso

## Autor

Proyecto universitario - Control de Elevador Inteligente (2026)

## Licencia

MIT
