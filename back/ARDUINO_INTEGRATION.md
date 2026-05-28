# Guía de Integración con Arduino

## 📋 Resumen

Este proyecto ahora soporta comunicación real con Arduino usando el patrón Adapter. Hay dos modos de operación:

1. **Modo Simulador** (desarrollo) - Sin hardware
2. **Modo Arduino** (producción) - Con hardware real

---

## 🔧 Configuración

### Cambiar entre Simulator y Arduino

Editar `src/main/resources/application.properties`:

```properties
# Para SIMULADOR (desarrollo)
spring.profiles.active=simulator
arduino.enabled=false

# Para ARDUINO (producción)
spring.profiles.active=arduino
arduino.enabled=true
arduino.port=COM3
```

### Identificar el Puerto Arduino

#### En Windows (PowerShell)
```powershell
Get-PnpDevice -PresentOnly | Where-Object { $_.Name -match "COM" }
# O ver en Administrador de dispositivos → Puertos (COM y LPT)
```

#### En Linux
```bash
ls /dev/tty*
# O probar con: dmesg | tail
```

#### En macOS
```bash
ls /dev/cu.* /dev/tty.*
```

---

## 🚀 Pasos para Usar Arduino Real

### 1️⃣ Preparar el Arduino

**Firmware Arduino Básico** (subir a tu Arduino):

```cpp
// Sketch de ejemplo para Arduino UNO/NANO/MEGA
// Baud Rate: 115200

void setup() {
  Serial.begin(115200);
  Serial.println("ARDUINO_ELEVATOR_READY");
  
  // Configurar pines para motores/sensores según tu hardware
  // pinMode(MOTOR_PIN, OUTPUT);
  // pinMode(SENSOR_PIN, INPUT);
}

int currentFloor = 1;
String doorState = "CLOSED";
String elevatorState = "IDLE";

void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    processCommand(command);
  }
}

void processCommand(String cmd) {
  cmd.trim();
  
  if (cmd == "PING") {
    Serial.println("PONG");
  }
  else if (cmd.startsWith("MOVE_TO_FLOOR")) {
    int floor = cmd.substring(14).toInt();
    if (floor >= 1 && floor <= 10) {
      currentFloor = floor;
      elevatorState = "MOVING";
      Serial.println("OK:MOVING:FLOOR_" + String(floor));
      delay(2000); // Simular movimiento
      elevatorState = "IDLE";
    } else {
      Serial.println("ERROR:003:Floor out of range");
    }
  }
  else if (cmd == "OPEN_DOOR") {
    doorState = "OPEN";
    elevatorState = "DOOR_OPENING";
    Serial.println("OK:DOOR_OPENING");
    delay(1000);
    elevatorState = "DOOR_OPEN";
  }
  else if (cmd == "CLOSE_DOOR") {
    doorState = "CLOSED";
    elevatorState = "DOOR_CLOSING";
    Serial.println("OK:DOOR_CLOSING");
    delay(800);
    elevatorState = "IDLE";
  }
  else if (cmd == "READ_STATE") {
    Serial.print("STATE:FLOOR=");
    Serial.print(currentFloor);
    Serial.print(",DOOR_STATE=");
    Serial.print(doorState);
    Serial.print(",ELEVATOR_STATE=");
    Serial.println(elevatorState);
  }
  else if (cmd == "RESET") {
    currentFloor = 1;
    doorState = "CLOSED";
    elevatorState = "IDLE";
    Serial.println("OK:RESET_COMPLETE");
  }
  else {
    Serial.println("ERROR:001:Unknown command");
  }
}
```

### 2️⃣ Identificar el Puerto

```bash
# Windows
cd back
Get-PnpDevice -PresentOnly | Where-Object { $_.Name -match "Arduino" }

# Linux
ls /dev/ttyUSB* /dev/ttyACM*

# macOS  
ls /dev/cu.usbserial-*
```

### 3️⃣ Actualizar Configuración

```properties
# application.properties
spring.profiles.active=arduino
arduino.port=COM3        # Cambiar según tu puerto
arduino.enabled=true
```

### 4️⃣ Compilar y Ejecutar

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run

# O con el script
./run.bat       # Windows
./run.sh        # Linux/macOS
```

---

## 📡 Protocolo de Comunicación

Ver archivo `ARDUINO_PROTOCOL.md` para documentación completa.

### Comandos Disponibles

| Comando | Parámetro | Respuesta | Descripción |
|---------|-----------|-----------|-------------|
| `PING` | - | `PONG` | Verificar conexión |
| `MOVE_TO_FLOOR` | 1-10 | `OK:MOVING:FLOOR_X` | Mover elevador |
| `OPEN_DOOR` | - | `OK:DOOR_OPENING` | Abrir puerta |
| `CLOSE_DOOR` | - | `OK:DOOR_CLOSING` | Cerrar puerta |
| `READ_STATE` | - | `STATE:FLOOR=X,...` | Leer estado |
| `RESET` | - | `OK:RESET_COMPLETE` | Resetear |

### Ejemplo de Comunicación

```
→ PING
← PONG

→ MOVE_TO_FLOOR 3
← OK:MOVING:FLOOR_3

→ READ_STATE
← STATE:FLOOR=3,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE

→ OPEN_DOOR
← OK:DOOR_OPENING

→ CLOSE_DOOR
← OK:DOOR_CLOSING
```

---

## 🔍 Debug y Troubleshooting

### Ver Logs Detallados

```properties
# application.properties
logging.level.co.edu.unillanos.elevator.infrastructure.adapter=DEBUG
logging.level.co.edu.unillanos.elevator.infrastructure.adapter.SerialPortManager=DEBUG
```

### Problemas Comunes

#### "Puerto no encontrado"
```
❌ Error: Puerto serial no encontrado: COM3
✓ Solución: Cambiar arduino.port en application.properties
```

#### "Timeout esperando respuesta"
```
❌ Error: Timeout esperando respuesta para comando: PING
✓ Soluciones:
  - Verificar que Arduino está encendido
  - Comprobar baudrate (115200)
  - Revisar cables USB
  - Reiniciar Arduino (DTR reset)
```

#### "ERROR:001:Unknown command"
```
❌ Error: Arduino devuelve comando no reconocido
✓ Solución: Verificar que el firmware Arduino tiene el comando
```

#### "Conexión se cierra después de conectar"
```
❌ Problema: Pérdida de conexión tras PING inicial
✓ Soluciones:
  - Agregar delay(1000) en setup() del Arduino
  - Desactivar autoflush en SerialPort
  - Revisar voltaje de alimentación
```

---

## 📊 Arquitectura

```
┌─────────────────┐
│  REST API       │
└────────┬────────┘
         │
    ┌────┴──────┐
    │ Service   │
    └────┬──────┘
         │
    ┌────┴──────────────┐
    │  HardwarePort     │ (interfaz)
    └────┬──┬───────────┘
         │  │
    ┌────▼──┴────┐
    │ Adapter    │
    ├────────────┤
    │Simulator   │ (modo desarrollo)
    │Arduino     │ (modo producción)
    └────┬───────┘
         │
    ┌────▼──────────────┐
    │ SerialPortManager │
    │ jSerialComm      │
    └────┬──────────────┘
         │
    ┌────▼────────┐
    │  UART Serial│
    └────┬────────┘
         │
    ┌────▼────────┐
    │   Arduino   │
    │  (hardware) │
    └─────────────┘
```

---

## 🧪 Pruebas Manuales

### Con cURL

```bash
# Obtener estado actual
curl http://localhost:8080/api/v1/elevators/1/state

# Mover a piso 3
curl -X POST http://localhost:8080/api/v1/elevators/1/move \
  -H "Content-Type: application/json" \
  -d '{"floor": 3}'

# Abrir puerta
curl -X POST http://localhost:8080/api/v1/elevators/1/door/open

# Cerrar puerta
curl -X POST http://localhost:8080/api/v1/elevators/1/door/close
```

### Con test-api.bat (Windows)

```bash
./test-api.bat
```

---

## 📌 Próximos Pasos Recomendados

1. **Implementar feedback en tiempo real**: Agregar eventos cuando Arduino notifique cambios de estado
2. **Sensores reales**: Conectar sensores de posición, peso, movimiento
3. **Control de motores**: Implementar control PWM para motores DC/stepper
4. **Seguridad**: Agregar sensores de emergencia, obstáculos
5. **Logging persistente**: Guardar eventos en base de datos
6. **Dashboard**: Visualización en tiempo real del estado del elevador

---

## 📚 Documentación Relacionada

- [ARDUINO_PROTOCOL.md](ARDUINO_PROTOCOL.md) - Especificación del protocolo
- [API_REST.md](API_REST.md) - Documentación de la API REST
- [ARCHITECTURE.md](ARCHITECTURE.md) - Arquitectura del proyecto
