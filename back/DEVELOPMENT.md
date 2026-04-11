# DEVELOPMENT GUIDE - Smart Elevator

## Estructura de Directorios Explicada

```
ELEVATOR_2026/
├── src/
│   ├── main/
│   │   ├── java/co/edu/unillanos/elevator/
│   │   │   ├── domain/              ← DOMINIO: Lógica pura, sin Spring
│   │   │   │   ├── model/          ← Entidades
│   │   │   │   ├── enums/          ← Valores enumerados
│   │   │   │   └── exception/      ← Excepciones de negocio
│   │   │   │
│   │   │   ├── application/         ← APLICACIÓN: Casos de uso y puertos
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/         ← Puertos de entrada (interfaces)
│   │   │   │   │   └── out/        ← Puertos de salida (interfaces)
│   │   │   │   └── service/        ← Servicios que orquestan
│   │   │   │
│   │   │   ├── infrastructure/      ← INFRAESTRUCTURA: Implementaciones concretas
│   │   │   │   ├── adapter/        ← Adaptadores (SimulatorAdapter, ArduinoAdapter)
│   │   │   │   └── config/         ← Configuración de Spring
│   │   │   │
│   │   │   ├── ui/                 ← UI: Interfaz de usuario
│   │   │   │   └── console/        ← Aplicación de consola
│   │   │   │
│   │   │   └── ElevatorApplication.java  ← MainClass
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/co/edu/unillanos/elevator/
│           ├── domain/ElevatorTest.java         ← Tests sin Spring
│           └── application/ElevatorServiceTest.java  ← Tests con Spring
│
├── pom.xml                          ← Dependencias Maven
├── QUICK_START.md                   ← Instrucciones básicas
├── ARCHITECTURE.md                  ← Explicación de arquitectura
├── README.md                        ← Documentación completa
├── run.bat                          ← Script Windows
├── run.sh                           ← Script Linux/Mac
└── .gitignore                       ← Git ignore
```

## Cómo Modificar/Extender

### 1. Agregar nueva regla de negocio

**Ubicación**: `domain/model/Elevator.java`

Ejemplo: Limitar velocidad de elevador a 2 pisos por segundo
```java
// En Elevator.java
public void goToFloor(int floor) {
    // ... validaciones existentes ...
    
    // NUEVA REGLA: Máximo 2 pisos
    int maxMove = 2;
    if (Math.abs(floor - currentFloor) > maxMove) {
        throw new ElevatorException(
            String.format("Movimiento máximo: %d pisos", maxMove)
        );
    }
    
    // ... resto del método ...
}
```

Luego actualizar tests en `ElevatorTest.java`.

### 2. Agregar nuevo evento loggeable

**Ubicación**: `application/service/ElevatorService.java`

```java
// En goToFloor()
public void goToFloor(int floor) {
    try {
        // ... código existente ...
        
        // EVENTO NUEVO
        eventLogger.logEvent("MOVED", "floor=" + reading.getFloor(), reading);
        
    } catch (ElevatorException e) {
        // ... manejo de error ...
    }
}
```

### 3. Agregar nuevo comando de consola

**Ubicación**: `ui/console/CommandParser.java`

```java
public boolean parseAndExecute(String input, ElevatorUseCase elevatorUseCase, DoorUseCase doorUseCase) {
    String[] parts = input.split("\\s+");
    String command = parts[0].toUpperCase();
    
    try {
        switch (command) {
            // ... casos existentes ...
            
            case "STATUS":  // NUEVO COMANDO
                SensorReading reading = elevatorUseCase.readSensors();
                log.info("[INFO] Status | Floor: {} | Door: {} | State: {}",
                    reading.getFloor(), reading.getDoorState(), reading.getElevatorState());
                return true;
                
            default:
                log.error("[ERROR] Comando desconocido: {}", command);
                return true;
        }
    } catch (Exception e) {
        log.error("Error: {}", e.getMessage());
        return true;
    }
}
```

### 4. Implementar ArduinoAdapter real

**Ubicación**: `infrastructure/adapter/ArduinoAdapter.java`

Reemplazar las excepciones con comunicación serial:

```java
@Component
@Profile("arduino")
public class ArduinoAdapter implements HardwarePort {
    
    private SerialPort serialPort;
    private static final String BAUD_RATE = "9600";
    private static final int TIMEOUT_MS = 1000;
    
    public ArduinoAdapter() {
        // Buscar puerto Arduino
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            if (port.getDescriptivePortName().contains("Arduino")) {
                this.serialPort = port;
                this.serialPort.setComPortParameters(9600, 8, 1, 0);
                this.serialPort.setComPortTimeouts(TIMEOUT_MS, TIMEOUT_MS, TIMEOUT_MS);
                this.serialPort.openPort();
                break;
            }
        }
    }
    
    @Override
    public void moveToFloor(int floor) {
        String command = "GO " + floor;
        sendCommand(command);
    }
    
    private void sendCommand(String command) {
        byte[] buffer = (command + "\n").getBytes();
        serialPort.writeBytes(buffer, buffer.length);
    }
    
    private String readResponse() throws InterruptedException {
        byte[] buffer = new byte[1024];
        int numRead = serialPort.readBytes(buffer, buffer.length);
        return new String(buffer, 0, numRead);
    }
}
```

### 5. Agregar nueva capa (REST API)

1. Agregar dependencia en `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

2. Crear `ui/rest/ElevatorController.java`:
```java
@RestController
@RequestMapping("/api/elevator")
@RequiredArgsConstructor
public class ElevatorController {
    
    private final ElevatorUseCase elevatorUseCase;
    private final DoorUseCase doorUseCase;
    
    @PostMapping("/go/{floor}")
    public ResponseEntity<?> goToFloor(@PathVariable int floor) {
        elevatorUseCase.goToFloor(floor);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/status")
    public ResponseEntity<SensorReading> getStatus() {
        return ResponseEntity.ok(elevatorUseCase.readSensors());
    }
}
```

3. Acceder en http://localhost:8080/api/elevator/status

## Best Practices

### 1. Mantener Domain limpio
- ✅ POJO sin anotaciones Spring
- ✅ Solo Lombok permitido
- ❌ No usar @Autowired en dominio
- ❌ No llamar a servicios desde dominio

### 2. Separación de responsabilidades
- Domain: Qué puede/no puede hacer (validación)
- Application: Cómo se coordina (orquestación)
- Infrastructure: Dónde/cómo se ejecuta (implementación)
- UI: Cómo interactúa el usuario

### 3. Testing
- Unit tests para dominio (sin Spring)
- Integration tests para servicios (con Spring)
- Mocks para adaptadores externos

### 4. Logging
- `log.info()` para eventos normales
- `log.error()` para errores
- `log.debug()` para debugging
- `eventLogger` para eventos de negocio

### 5. Excepciones
- Usar `ElevatorException` para violaciones de reglas
- Lanzar en dominio, capturar en servicios
- Loguear errores en servicios

## Checklist para Pull Requests

- [ ] Código compila sin errores
- [ ] Tests pasan (ElevatorTest + ElevatorServiceTest)
- [ ] Dominio sigue siendo POJO puro
- [ ] Servicios usan inyección por constructor
- [ ] Javadoc en clases/interfaces públicas
- [ ] Comments en código complejo (español)
- [ ] Formato: 4 espacios, no tabs
- [ ] Sin `System.out.println()` (usar logging)
- [ ] Sin TODO comentarios vencidos

## Depuración

### Ver qué capa está fallando

1. **Error en ElevatorTest**
   - Problema en dominio (Elevator.java)
   - Revisar lógica de máquina de estados

2. **Error en ElevatorServiceTest**
   - Problema en servicios o adaptadores
   - Verificar inyección de dependencias

3. **Error en ConsoleApp**
   - Problema en parsing de comandos o UI
   - Revisar CommandParser

### Habilitar debug logging

En `application.properties`:
```properties
logging.level.co.edu.unillanos.elevator=DEBUG
```

O en línea de comandos:
```bash
java -jar smart-elevator-1.0.0.jar --logging.level.co.edu.unillanos.elevator=DEBUG
```

### Ver logs en tiempo real

```bash
tail -f elevator.log
```

## Próximas Mejoras (Roadmap)

- [ ] **Fase 1**: ArduinoAdapter funcional con jSerialComm
- [ ] **Fase 2**: REST API completa (CRUD operaciones)
- [ ] **Fase 3**: Base de datos (Historial de movimientos)
- [ ] **Fase 4**: UI Web (Tabla de control interactiva)
- [ ] **Fase 5**: Estadísticas (Tiempo medio de espera, movimientos/día)
- [ ] **Fase 6**: Planificador de ruta (Optimizar secuencia de pisos)
- [ ] **Fase 7**: Alarmas (Detección de anomalías)

## Recursos Útiles

- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

## ContactoDeudas Técnicas

⚠️ Cosas que necesitan atención:

1. **ArduinoAdapter es stub** - Implementar comunicación serial real
2. **Sin base de datos** - Agregar persistencia de eventos
3. **Sin validación de entrada** - Validar números, strings, etc.
4. **Sin timeout en movimientos** - Agregar detección de bloqueos
5. **Logging a archivo manual** - Considerar usar framework de logging

## FAQ - Preguntas Frecuentes

**P: ¿Por qué Domain no tiene Spring?**
R: Para que sea testeable sin Spring y reutilizable en otros contextos.

**P: ¿Cómo agrego un nuevo perfil?**
R: Copia un adapter, cambia @Profile, implementa HardwarePort.

**P: ¿Dónde agrego la lógica de negocio?**
R: En `domain/model/Elevator.java` o en nuevas clases dentro de `domain/model/`.

**P: ¿Cómo pruebo sin compilar el JAR?**
R: `mvn spring-boot:run` o ejecuta tests con `mvn test`.

**P: ¿Se puede cambiar de Simulator a Arduino sin recompilar?**
R: Sí, solo cambia el perfil en `application.properties` o línea de comandos.
