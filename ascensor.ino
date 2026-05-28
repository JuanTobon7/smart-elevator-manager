/**
 * ============================================
 * SMART ELEVATOR MANAGER - ARDUINO FIRMWARE
 * ============================================
 * 
 * CONTRATO DE COMUNICACIÓN SERIAL
 * ============================================
 * Baudrate:   115200 bps
 * Data bits:  8
 * Stop bits:  1
 * Parity:     NONE
 * Terminador: \n (LF)
 * 
 * COMANDOS DISPONIBLES:
 * 
 * 1. PING
 *    → Backend:  PING\n
 *    ← Arduino:  PONG\n
 * 
 * 2. MOVE_TO_FLOOR <FLOOR>
 *    → Backend:  MOVE_TO_FLOOR 3\n
 *    ← Arduino:  OK:MOVING:FLOOR_3\n
 *    Error:     ERROR:003:Floor out of range\n
 * 
 * 3. OPEN_DOOR
 *    → Backend:  OPEN_DOOR\n
 *    ← Arduino:  OK:DOOR_OPENING\n
 * 
 * 4. CLOSE_DOOR
 *    → Backend:  CLOSE_DOOR\n
 *    ← Arduino:  OK:DOOR_CLOSING\n
 * 
 * 5. READ_STATE
 *    → Backend:  READ_STATE\n
 *    ← Arduino:  STATE:FLOOR=3,DOOR_STATE=CLOSED,ELEVATOR_STATE=IDLE\n
 * 
 * 6. RESET
 *    → Backend:  RESET\n
 *    ← Arduino:  OK:RESET_COMPLETE\n
 * 
 * ESTADOS VÁLIDOS:
 * - FLOOR: 1-10
 * - ELEVATOR_STATE: IDLE, MOVING, DOOR_OPEN, DOOR_OPENING, DOOR_CLOSING
 * - DOOR_STATE: OPEN, CLOSED, OPENING, CLOSING
 * 
 * CÓDIGOS DE ERROR:
 * - 001: Comando no reconocido
 * - 002: Parámetros inválidos
 * - 003: Piso fuera de rango (1-10)
 * - 004: Puerta bloqueada
 * - 005: Sensor de peso excedido
 * - 006: Timeout en operación
 * - 007: Fallo de motor
 * - 008: Fallo de sensor
 * 
 * ============================================
 */

// ============================================
// CONFIGURACIÓN DE HARDWARE (PERSONALIZAR)
// ============================================
// Descomenta y configura según tu hardware
// const int MOTOR_UP = 5;        // PWM para motor arriba
// const int MOTOR_DOWN = 6;      // PWM para motor abajo
// const int SENSOR_FLOOR = A0;   // Sensor de posición
// const int SENSOR_DOOR = 7;     // Sensor de puerta
// const int RELAY_DOOR = 8;      // Relé para puerta

#include <Stepper.h>
// ============================================
// VARIABLES DE ESTADO
// ============================================
int currentFloor = 1;           // Piso actual (1-10)
String doorState = "CLOSED";    // Estado puerta
String elevatorState = "IDLE";  // Estado elevador
Stepper motor(2048, 8, 10, 9, 11);
// ============================================
// CONFIGURACIÓN INICIAL
// ============================================
void setup() {
  // Inicializar puerto serial a 115200 baud
  motor.setSpeed(10);
  Serial.begin(115200);
  
  // Esperar a que se estabilice la conexión
  delay(1000);
  
  
  // TODO: Configurar pines de hardware
 // pinMode(MOTOR_UP, OUTPUT);
  //pinMode(MOTOR_DOWN, OUTPUT);
  // pinMode(SENSOR_FLOOR, INPUT);
  // pinMode(SENSOR_DOOR, INPUT);
  // pinMode(RELAY_DOOR, OUTPUT);
  // digitalWrite(RELAY_DOOR, LOW);
}

// ============================================
// LOOP PRINCIPAL - PROCESAR COMANDOS
// ============================================
void loop() {
  // Leer comando del puerto serial
  if (Serial.available()) {
    // Leer hasta encontrar \n
    String command = Serial.readStringUntil('\n');
    
    // Eliminar espacios en blanco
    command.trim();
    
    // Procesar comando recibido
    if (command.length() > 0) {
      processCommand(command);
    }
  }
}

// ============================================
// PROCESAR COMANDO RECIBIDO
// ============================================
void processCommand(String cmd) {
  // PING - Verificar conexión
  if (cmd == "PING") {
    Serial.println("PONG");
  }
  
  // MOVE_TO_FLOOR <FLOOR> - Mover elevador
  else if (cmd.startsWith("MOVE_TO_FLOOR")) {
    int floor = cmd.substring(14).toInt();
    moveToFloor(floor);
  }
  
  // OPEN_DOOR - Abrir puerta
  else if (cmd == "OPEN_DOOR") {
    openDoor();
  }
  
  // CLOSE_DOOR - Cerrar puerta
  else if (cmd == "CLOSE_DOOR") {
    closeDoor();
  }
  
  // READ_STATE - Leer estado completo
  else if (cmd == "READ_STATE") {
    readState();
  }
  
  // RESET - Resetear a estado inicial
  else if (cmd == "RESET") {
    reset();
  }
  
  // Comando no reconocido
  else {
    Serial.println("ERROR:001:Unknown command");
  }
}

// ============================================
// MOVER ELEVADOR A PISO
// ============================================
void moveToFloor(int targetFloor) {

  if (targetFloor < 1 || targetFloor > 10) {
    Serial.println("ERROR:003:Floor out of range");
    return;
  }

  if (doorState != "CLOSED") {
    Serial.println("ERROR:004:Door not closed");
    return;
  }

  if (targetFloor == currentFloor) {
    Serial.print("OK:ALREADY_AT:FLOOR_");
    Serial.println(targetFloor);
    return;
  }

  elevatorState = "MOVING";

  int direction = (targetFloor > currentFloor) ? 1 : -1;
  int mover_piso= 1 * 2048;
  while (currentFloor != targetFloor) {
    
    // Mover un piso
    motor.step(direction * mover_piso);  // Ajusta según tu motor

    delay(2000); // 2 segundos por piso (simulación)

    currentFloor += direction;

    Serial.print("MOVING:NOW_AT_FLOOR_");
    Serial.println(currentFloor);
  }

  elevatorState = "IDLE";

  Serial.print("OK:ARRIVED:FLOOR_");
  Serial.println(currentFloor);
}

// ============================================
// ABRIR PUERTA DEL ELEVADOR
// ============================================
void openDoor() {
  // No abrir si elevador está en movimiento
  if (elevatorState != "IDLE") {
    Serial.println("ERROR:006:Elevator moving");
    return;
  }
  
  // Actualizar estado
  doorState = "OPENING";
  elevatorState = "DOOR_OPENING";
  
  // Enviar confirmación
  Serial.println("OK:DOOR_OPENING");
  
  // ============================================
  // AQUÍ IMPLEMENTAR CONTROL DE PUERTA REAL
  // ============================================
  // Ejemplos:
  // - Activar motor de puerta: digitalWrite(RELAY_DOOR, HIGH)
  // - Esperar sensor de puerta abierta
  // - Usar PWM para control suave: analogWrite(MOTOR_PUERTA, 200)
  
  // SIMULACIÓN: esperar 1 segundo
  delay(1000);
  
  // Puerta abierta
  doorState = "OPEN";
  elevatorState = "DOOR_OPEN";
}

// ============================================
// CERRAR PUERTA DEL ELEVADOR
// ============================================
void closeDoor() {
  // Actualizar estado
  doorState = "CLOSING";
  elevatorState = "DOOR_CLOSING";
  
  // Enviar confirmación
  Serial.println("OK:DOOR_CLOSING");
  
  // ============================================
  // AQUÍ IMPLEMENTAR CONTROL DE PUERTA REAL
  // ============================================
  // Ejemplos:
  // - Desactivar motor de puerta: digitalWrite(RELAY_DOOR, LOW)
  // - Esperar sensor de puerta cerrada
  // - Usar PWM para control suave: analogWrite(MOTOR_PUERTA, 100)
  
  // SIMULACIÓN: esperar 0.8 segundos
  delay(800);
  
  // Puerta cerrada
  doorState = "CLOSED";
  elevatorState = "IDLE";
}

// ============================================
// LEER ESTADO ACTUAL DEL ELEVADOR
// ============================================
void readState() {
  // Formato: STATE:FLOOR=X,DOOR_STATE=Y,ELEVATOR_STATE=Z\n
  Serial.print("STATE:FLOOR=");
  Serial.print(currentFloor);
  Serial.print(",DOOR_STATE=");
  Serial.print(doorState);
  Serial.print(",ELEVATOR_STATE=");
  Serial.println(elevatorState);
}

// ============================================
// RESETEAR A ESTADO INICIAL
// ============================================
void reset() {
  // Volver a estado inicial
  currentFloor = 1;
  doorState = "CLOSED";
  elevatorState = "IDLE";
  
  // ============================================
  // AQUÍ IMPLEMENTAR RESET DE HARDWARE
  // ============================================
  // Ejemplos:
  // - Apagar todos los motores: digitalWrite(MOTOR_UP, LOW)
  // - Moverse a piso 1 usando sensores
  // - Calibrar sensores
  
  // Enviar confirmación
  Serial.println("OK:RESET_COMPLETE");
}

// ============================================
// FUNCIONES AUXILIARES (PERSONALIZAR)
// ============================================

/**
 * Leer posición actual del elevador desde sensor
 * Descomentar cuando tengas sensor conectado
 */
// int getFloorFromSensor() {
//   int sensorValue = analogRead(SENSOR_FLOOR);
//   // Mapear valor analógico (0-1023) a piso (1-10)
//   return map(sensorValue, 0, 1023, 1, 10);
// }

/**
 * Verificar si puerta está abierta
 * Descomentar cuando tengas sensor de puerta
 */
// boolean isDoorOpen() {
//   return digitalRead(SENSOR_DOOR) == HIGH;
// }

/**
 * Mover motor hacia arriba
 * Descomentar cuando tengas motor conectado
 */
// void moveMotorUp() {
//   digitalWrite(MOTOR_DOWN, LOW);
//   analogWrite(MOTOR_UP, 255);  // Velocidad máxima
// }

/**
 * Mover motor hacia abajo
 * Descomentar cuando tengas motor conectado
 */
// void moveMotorDown() {
//   digitalWrite(MOTOR_UP, LOW);
//   analogWrite(MOTOR_DOWN, 255);  // Velocidad máxima
// }

/**
 * Detener motor
 * Descomentar cuando tengas motor conectado
 */
// void stopMotor() {
//   digitalWrite(MOTOR_UP, LOW);
//   digitalWrite(MOTOR_DOWN, LOW);
// }

// ============================================
// FIN DEL CÓDIGO
// ============================================
