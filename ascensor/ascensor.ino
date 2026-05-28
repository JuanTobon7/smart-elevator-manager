/**
 * ============================================
 * SMART ELEVATOR MANAGER - ARDUINO FIRMWARE
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
const int SENSOR_PUERTA = 13;   // MH Flying Fish — LOW: detecta objeto | HIGH: libre
const int LED_VERDE      = 6;   // Puerta cerrada (paso bloqueado confirmado)
const int LED_ROJO       = 3; 

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
  
  pinMode(SENSOR_PUERTA, INPUT);
  pinMode(LED_VERDE,     OUTPUT);
  pinMode(LED_ROJO,      OUTPUT);

  // Esperar a que se estabilice la conexión
  actualizarIndicadores();

  delay(1000);
  
  
}

// ============================================
// LOOP PRINCIPAL - PROCESAR COMANDOS
// ============================================
void loop() {

  actualizarIndicadores();

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

void actualizarIndicadores() {
  bool libreDeObstaculo = (digitalRead(SENSOR_PUERTA) == HIGH);

  if (libreDeObstaculo) {
    // Sin detección → simulamos puerta abierta
    digitalWrite(LED_ROJO,   HIGH);
    digitalWrite(LED_VERDE,  LOW);
    doorState = "OPEN";
  } else {
    // Objeto detectado → simulamos puerta cerrada
    digitalWrite(LED_VERDE,  HIGH);
    digitalWrite(LED_ROJO,   LOW);
    doorState = "CLOSED";
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

  actualizarIndicadores();
  
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
//  Espera hasta que el sensor confirme vano libre (puerta abierta).
//  Timeout de 5 s para evitar bloqueos.
// ============================================
void openDoor() {
  if (elevatorState != "IDLE") {
    Serial.println("ERROR:006:Elevator moving");
    return;
  }

  doorState     = "OPENING";
  elevatorState = "DOOR_OPENING";
  Serial.println("OK:DOOR_OPENING");

  // Encender LED rojo anticipadamente (indicando que se está abriendo)
  digitalWrite(LED_ROJO,  HIGH);
  digitalWrite(LED_VERDE, LOW);

  // ── Aquí iría el control real del actuador de puerta ──

  // Esperar confirmación del sensor (timeout 5 s)
  unsigned long inicio = millis();
  while (digitalRead(SENSOR_PUERTA) == LOW) {   // Mientras siga detectando objeto
    if (millis() - inicio > 5000) {
      Serial.println("ERROR:006:Timeout opening door");
      elevatorState = "IDLE";
      actualizarIndicadores();
      return;
    }
    delay(50);
  }

  // Sensor confirma: vano libre → puerta abierta
  doorState     = "OPEN";
  elevatorState = "DOOR_OPEN";
  digitalWrite(LED_ROJO,  HIGH);
  digitalWrite(LED_VERDE, LOW);
}

// ============================================
// CERRAR PUERTA DEL ELEVADOR
// ============================================
//  Espera hasta que el sensor detecte objeto (puerta cerrada).
//  Timeout de 5 s para evitar bloqueos.
// ============================================
void closeDoor() {
  doorState     = "CLOSING";
  elevatorState = "DOOR_CLOSING";
  Serial.println("OK:DOOR_CLOSING");

  // Encender LED verde anticipadamente (indicando que se está cerrando)
  digitalWrite(LED_VERDE, HIGH);
  digitalWrite(LED_ROJO,  LOW);

  // ── Aquí iría el control real del actuador de puerta ──

  // Esperar confirmación del sensor (timeout 5 s)
  unsigned long inicio = millis();
  while (digitalRead(SENSOR_PUERTA) == HIGH) {  // Mientras NO detecte objeto
    if (millis() - inicio > 5000) {
      Serial.println("ERROR:006:Timeout closing door");
      elevatorState = "IDLE";
      actualizarIndicadores();
      return;
    }
    delay(50);
  }

  // Sensor confirma: objeto detectado → puerta cerrada
  doorState     = "CLOSED";
  elevatorState = "IDLE";
  digitalWrite(LED_VERDE, HIGH);
  digitalWrite(LED_ROJO,  LOW);
}

// ============================================
// LEER ESTADO ACTUAL DEL ELEVADOR
// ============================================
void readState() {
  // Formato: STATE:FLOOR=X,DOOR_STATE=Y,ELEVATOR_STATE=Z\n
  actualizarIndicadores();

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
  
  
  actualizarIndicadores();

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
