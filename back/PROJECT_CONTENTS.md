# PROJECT CONTENTS - Smart Elevator

## Listado Completo de Archivos

### Configuración
```
pom.xml                          Maven build configuration
.gitignore                       Git ignore patterns
```

### Documentación
```
README.md                        Documentación completa del proyecto
QUICK_START.md                   Guía de inicio rápido
ARCHITECTURE.md                  Explicación de arquitectura hexagonal
DEVELOPMENT.md                   Guía de desarrollo
PROJECT_CONTENTS.md              Este archivo
```

### Scripts
```
run.bat                          Ejecutar en Windows
run.sh                           Ejecutar en Linux/Mac
```

### Código Fuente Principal (`src/main/java/...`)

**Domain Layer** (Lógica pura sin Spring)
```
domain/
├── enums/
│   ├── Direction.java              UP, DOWN, NONE
│   ├── DoorState.java              OPEN, CLOSED, CLOSING
│   └── ElevatorState.java          IDLE, MOVING, DOOR_OPEN, DOOR_CLOSING, ERROR
├── exception/
│   └── ElevatorException.java       Excepciones de dominio
└── model/
    ├── Door.java                   Entidad: puerta
    ├── Elevator.java               Entidad principal: máquina de estados
    ├── Floor.java                  Value object: piso (1-5)
    └── SensorReading.java          DTO: lectura de sensores
```

**Application Layer** (Casos de uso y puertos)
```
application/
├── port/
│   ├── in/
│   │   ├── DoorUseCase.java        Interfaz: open(), close()
│   │   └── ElevatorUseCase.java    Interfaz: goToFloor(), readSensors(), reset()
│   └── out/
│       ├── EventLogger.java        Interfaz: logging de eventos
│       └── HardwarePort.java       Interfaz: control de hardware
└── service/
    ├── DoorService.java            Implementa DoorUseCase
    └── ElevatorService.java        Implementa ElevatorUseCase
```

**Infrastructure Layer** (Implementaciones concretas)
```
infrastructure/
├── adapter/
│   ├── ArduinoAdapter.java         Stub para Arduino (jSerialComm en futuro)
│   ├── FileEventLogger.java        Implementa EventLogger (archivo)
│   └── SimulatorAdapter.java       Implementa HardwarePort (simulador)
└── config/
    └── ElevatorConfig.java         Configuración Spring (factory beans)
```

**UI Layer** (Interfaz de usuario)
```
ui/
└── console/
    ├── CommandParser.java          Parser de comandos de consola
    └── ConsoleApp.java             Aplicación de consola (CommandLineRunner)
```

**Aplicación Principal**
```
ElevatorApplication.java           MainClass de Spring Boot
```

### Recursos (`src/main/resources/`)
```
application.properties             Configuración de perfil y logging
```

### Tests (`src/test/java/...`)

**Unit Tests** (Sin Spring)
```
domain/
└── ElevatorTest.java              Tests de máquina de estados
    - testInitialState()
    - testValidMovement()
    - testMoveWithOpenDoor()
    - testOpenDoor()
    - testCloseDoor()
    - testCompleteDoorClosing()
    - testResetFromError()
    - testInvalidFloorZero()
    - testInvalidFloorSix()
    - testReadSensors()
    - testGoToCurrentFloor()
    - testDirectionDown()
```

**Integration Tests** (Con Spring + SimulatorAdapter)
```
application/
└── ElevatorServiceTest.java       Tests de integración
    - testGoToFloor()
    - testCompleteSequence()
    - testReset()
    - testMovementToFloor5()
    - testMultipleDoorSequences()
```

## Estadísticas del Proyecto

### Líneas de Código

```
Domain Layer:        ~250 LOC  (validaciones + máquina de estados)
Application Layer:   ~320 LOC  (servicios + lógica de orquestación)
Infrastructure:      ~300 LOC  (adaptadores + config)
UI Layer:            ~150 LOC  (consola + parser)
Tests:               ~300 LOC  (12 test cases)
─────────────────────────────────
TOTAL:              ~1320 LOC
```

### Clases por Capa

| Capa | Clases | Interfaces | Enums | Exceptions |
|------|--------|-----------|-------|-----------|
| Domain | 4 | 0 | 3 | 1 |
| Application | 4 | 2 | 0 | 0 |
| Infrastructure | 4 | 0 | 0 | 0 |
| UI | 2 | 0 | 0 | 0 |
| Main | 1 | 0 | 0 | 0 |
| **TOTAL** | **15** | **2** | **3** | **1** |

### Dependencias

**Spring Boot**
- spring-boot-starter (Core)
- spring-boot-starter-logging (SLF4J)
- spring-boot-starter-test (JUnit 5)

**Third-party**
- Lombok (Reducción de boilerplate)
- jSerialComm (Para Arduino en futuro)

**Build**
- Maven 3.6+
- Java 17+

## Comandos Útiles

### Compilar
```bash
mvn clean compile                  # Compilar solo
mvn clean package                  # Compilar + JAR
mvn clean package -DskipTests      # Compilar + JAR sin tests
```

### Ejecutar
```bash
mvn spring-boot:run                # Maven (desarrollo)
java -jar target/smart-elevator-1.0.0.jar  # JAR directo
run.bat                            # Script Windows
./run.sh                           # Script Linux/Mac
```

### Tests
```bash
mvn test                           # Todos los tests
mvn test -Dtest=ElevatorTest       # Solo tests UnitTests
mvn test -Dtest=ElevatorServiceTest # Solo integration tests
```

### Logging
```bash
tail -f elevator.log               # Ver logs en tiempo real
cat elevator.log                   # Ver histórico de logs
```

## Perfiles de Spring

| Perfil | Descripción | HardwarePort | Cuándo usar |
|--------|-------------|--------------|-----------|
| `simulator` | Simulador sin Arduino | SimulatorAdapter | Desarrollo, Demo, Testing |
| `arduino` | Hardware real (stub) | ArduinoAdapter | Futuro con Arduino real |
| `test` | Tests sin ConsoleApp | SimulatorAdapter | Automático en mvn test |

Activar:
```bash
# application.properties
spring.profiles.active=simulator

# Línea de comandos
java -jar app.jar --spring.profiles.active=simulator
```

## Máquina de Estados Implementada

**Estados**: 5
- IDLE (parado, listo)
- MOVING (en movimiento)
- DOOR_OPEN (puerta abierta)
- DOOR_CLOSING (puerta cerrándose)
- ERROR (estado de error)

**Transiciones**: 8
```
IDLE → MOVING: goToFloor(n) con n != currentFloor y puerta CLOSED
MOVING → IDLE: arriveAtFloor()
IDLE → DOOR_OPEN: openDoor()
DOOR_OPEN → DOOR_CLOSING: closeDoor()
DOOR_CLOSING → IDLE: completeDoorClosing()
IDLE → ERROR: violación de reglas
ERROR → IDLE: reset()
ANY → ERROR: excepciones no controladas
```

**Reglas de Negocio**: 6
1. No mover con puerta abierta
2. No abrir puerta excepto en IDLE
3. No cerrar puerta si no está abierta
4. No completar cierre si no está cerrando
5. Pisos válidos: 1-5 solamente
6. Desde ERROR solo se puede reset()

## Patrones Utilizados

| Patrón | Ubicación | Propósito |
|--------|-----------|-----------|
| Hexagonal/Ports & Adapters | Toda la app | Arquitectura |
| State Machine | Elevator.java | Control de estados |
| Dependency Injection | Services | Inyección de dependendencias |
| Strategy | Adapters | Intercambiabilidad de estrategias |
| Repository | (Future) | Acceso a datos |
| Factory | ElevatorConfig | Creación de beans |
| Adapter | Infrastructure | Traducción de interfaces |
| Value Object | Floor.java | Objetos de valor validados |

## Futuros Desarrollos

**Corto Plazo** (semana 1)
- [ ] ArduinoAdapter funcional
- [ ] Más tests de casos límite

**Mediano Plazo** (2-4 semanas)
- [ ] REST API completa
- [ ] Base de datos (Histórico)
- [ ] Página web básica

**Largo Plazo** (mes+)
- [ ] Dashboard en tiempo real
- [ ] Planificador de ruta
- [ ] Sistema de alarmas
- [ ] Analytics

## Archivos de Salida

Los siguientes archivos se generan al ejecutar:

```
target/
├── smart-elevator-1.0.0.jar          JAR ejecutable
├── smart-elevator-1.0.0.jar.original Original sin shade
└── smart-elevator-1.0.0-sources.jar   Sources JAR (si se especifica)

elevator.log                           Registro de eventos (se crea al ejecutar)
```

## Cómo Empezar

1. **Clone/Descarga el proyecto**
2. **Lee QUICK_START.md** (2 minutos)
3. **Compila**: `mvn clean package`
4. **Ejecuta**: `java -jar target/smart-elevator-1.0.0.jar`
5. **Escribe comandos**: `GO 3`, `DOOR OPEN`, `READ`, `EXIT`

## Estructura Visual Total

```
smart-elevator/
├── src/
│   ├── main/java/co/edu/unillanos/elevator/
│   │   ├── domain/[model, enums, exception]      <-- Lógica pura
│   │   ├── application/[port/in, port/out, service]  <-- Orquestación
│   │   ├── infrastructure/[adapter, config]     <-- Concreto
│   │   ├── ui/console/                           <-- Interfaz
│   │   └── ElevatorApplication.java
│   ├── main/resources/application.properties
│   └── test/java/[domain, application]/
├── pom.xml
├── README.md + QUICK_START.md + ARCHITECTURE.md + DEVELOPMENT.md
├── run.bat + run.sh
└── .gitignore
```

---

**Última actualización**: Abril 2026
**Versión**: 1.0.0
**Status**: ✅ Funcional (modo Simulator listo, ArduinoAdapter es stub)
