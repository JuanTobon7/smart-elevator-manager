# Protocolo de Comunicación Arduino-Backend

## Configuración Serial
- **Baudrate**: 115200 bps
- **Data bits**: 8
- **Stop bits**: 1
- **Parity**: NONE
- **Flow Control**: NONE
- **Terminador**: `\n` (LF)

---

## Formato de Mensajes

### Estructura General
```
<COMANDO> <PARÁMETROS>\n
```

Todos los comandos terminan con salto de línea (`\n`).
Las respuestas del Arduino también terminan con `\n`.

---

## Comandos Backend → Arduino

### 1. **MOVE_TO_FLOOR**
**Propósito**: Mover el elevador a un piso específico

```
MOVE_TO_FLOOR <FLOOR>\n
```

**Ejemplo**:
```
MOVE_TO_FLOOR 3\n
```

**Respuesta esperada**:
```
OK:MOVING:FLOOR_3\n
```

---

### 2. **OPEN_DOOR**
**Propósito**: Abrir la puerta del elevador

```
OPEN_DOOR\n
```

**Respuesta esperada**:
```
OK:DOOR_OPENING\n
```

---

### 3. **CLOSE_DOOR**
**Propósito**: Cerrar la puerta del elevador

```
CLOSE_DOOR\n
```

**Respuesta esperada**:
```
OK:DOOR_CLOSING\n
```

---

### 4. **READ_STATE**
**Propósito**: Leer el estado actual del elevador

```
READ_STATE\n
```

**Respuesta esperada**:
```
STATE:FLOOR=2,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE\n
```

---

### 5. **RESET**
**Propósito**: Resetear el elevador al estado inicial

```
RESET\n
```

**Respuesta esperada**:
```
OK:RESET_COMPLETE\n
```

---

### 6. **PING**
**Propósito**: Verificar conexión con Arduino

```
PING\n
```

**Respuesta esperada**:
```
PONG\n
```

---

## Estados Posibles

### ELEVATOR_STATE
- `IDLE` - Elevador en reposo
- `MOVING` - Elevador en movimiento
- `DOOR_OPEN` - Puerta abierta
- `DOOR_OPENING` - Puerta abriéndose
- `DOOR_CLOSING` - Puerta cerrándose

### DOOR_STATE
- `OPEN` - Puerta abierta
- `CLOSED` - Puerta cerrada
- `OPENING` - Puerta en proceso de apertura
- `CLOSING` - Puerta en proceso de cierre

---

## Manejo de Errores

### Respuesta de Error
```
ERROR:<CÓDIGO>:<MENSAJE>\n
```

**Códigos de Error**:
- `001` - Comando no reconocido
- `002` - Parámetros inválidos
- `003` - Piso fuera de rango
- `004` - Puerta bloqueada
- `005` - Sensor de peso excedido
- `006` - Timeout en operación
- `007` - Fallo de motor
- `008` - Fallo de sensor

**Ejemplo**:
```
ERROR:003:Floor out of range\n
```

---

## Ejemplo de Secuencia Completa

```
[Backend → Arduino] PING\n
[Arduino → Backend] PONG\n

[Backend → Arduino] READ_STATE\n
[Arduino → Backend] STATE:FLOOR=1,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE\n

[Backend → Arduino] MOVE_TO_FLOOR 5\n
[Arduino → Backend] OK:MOVING:FLOOR_5\n

[Arduino espera 4 segundos simulados]

[Backend → Arduino] READ_STATE\n
[Arduino → Backend] STATE:FLOOR=5,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE\n

[Backend → Arduino] OPEN_DOOR\n
[Arduino → Backend] OK:DOOR_OPENING\n

[Arduino espera 1 segundo simulado]

[Backend → Arduino] CLOSE_DOOR\n
[Arduino → Backend] OK:DOOR_CLOSING\n

[Arduino espera 0.8 segundos simulados]

[Backend → Arduino] READ_STATE\n
[Arduino → Backend] STATE:FLOOR=5,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE\n
```

---

## Firmware Arduino Básico (Referencia)

```cpp
void setup() {
  Serial.begin(115200);
  Serial.println("ARDUINO_ELEVATOR_READY");
}

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
    moveToFloor(floor);
  }
  else if (cmd == "OPEN_DOOR") {
    openDoor();
  }
  else if (cmd == "CLOSE_DOOR") {
    closeDoor();
  }
  else if (cmd == "READ_STATE") {
    readState();
  }
  else if (cmd == "RESET") {
    reset();
  }
  else {
    Serial.println("ERROR:001:Unknown command");
  }
}
```

---

## Notas de Implementación

1. **Timeout de Lectura**: 5 segundos por defecto
2. **Reintentos**: 3 intentos antes de fallar
3. **Validación**: Siempre validar rango de pisos (1-10)
4. **Seguridad**: No permitir movimiento si puerta está abierta
5. **Thread-Safe**: Usar synchronized o locks para operaciones serial
6. **Buffering**: Usar buffer interno en caso de comandos rápidos consecutivos
