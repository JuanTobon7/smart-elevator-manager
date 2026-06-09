#include <Stepper.h>

const int SENSOR_PUERTA = 7;
const int SENSOR_PISO = 13;
const int SENSOR_ACTIVO = LOW;

const int LED_PUERTA_VERDE = 4;
const int LED_PUERTA_ROJO = 3;
const int LED_PISO_ROJO_MOV = 5;
const int LED_PISO_ROJO_STOP = 6;

const int VELOCIDAD_NORMAL = 10;
const int PASOS_POR_CICLO = 64;

int currentFloor = 1;
String doorState = "CLOSED";
String elevatorState = "IDLE";
bool emergencyStop = false;
bool ultimoEstado;

Stepper motor(2048, 8, 10, 9, 11);

void setup() {
  Serial.begin(115200);
  motor.setSpeed(VELOCIDAD_NORMAL);

  pinMode(SENSOR_PUERTA, INPUT);
  pinMode(SENSOR_PISO, INPUT);
  pinMode(LED_PUERTA_VERDE, OUTPUT);
  pinMode(LED_PUERTA_ROJO, OUTPUT);
  pinMode(LED_PISO_ROJO_MOV, OUTPUT);
  pinMode(LED_PISO_ROJO_STOP, OUTPUT);

  ultimoEstado = digitalRead(SENSOR_PISO);
  actualizarIndicadores();
  Serial.println("SMART ELEVATOR READY - INICIADO EN PISO 1");
}

void loop() {
  actualizarIndicadores();

  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();
    if (command.length() > 0) {
      processCommand(command);
    }
  }
}

void actualizarIndicadores() {
  bool puertaCerrada = (digitalRead(SENSOR_PUERTA) == SENSOR_ACTIVO);
  digitalWrite(LED_PUERTA_VERDE, puertaCerrada ? HIGH : LOW);
  digitalWrite(LED_PUERTA_ROJO, puertaCerrada ? LOW : HIGH);

  bool enPiso = (digitalRead(SENSOR_PISO) == SENSOR_ACTIVO);
  digitalWrite(LED_PISO_ROJO_MOV, enPiso ? LOW : HIGH);
  digitalWrite(LED_PISO_ROJO_STOP, enPiso ? HIGH : LOW);
}

void processCommand(String cmd) {
  if (cmd == "PING") {
    Serial.println("PONG");
  } else if (cmd.startsWith("MOVE_TO_FLOOR")) {
    int floor = cmd.substring(14).toInt();
    moveToFloor(floor);
  } else if (cmd == "OPEN_DOOR") {
    openDoor();
  } else if (cmd == "CLOSE_DOOR") {
    closeDoor();
  } else if (cmd == "READ_STATE") {
    readState();
  } else if (cmd == "EMERGENCY_STOP") {
    handleEmergencyStop();
  } else if (cmd == "RESET") {
    reset();
  } else if (cmd.startsWith("BAJAR")) {
    int pasos = cmd.substring(6).toInt();
    motor.setSpeed(VELOCIDAD_NORMAL);
    Serial.print("BAJANDO ");
    Serial.print(pasos);
    Serial.println(" PASOS...");
    long restantes = pasos;
    while (restantes > 0) {
      int bloque = min((long)PASOS_POR_CICLO, restantes);
      motor.step(-1 * bloque);
      restantes -= bloque;
    }
    Serial.println("LISTO");
  } else if (cmd.startsWith("SUBIR")) {
    int pasos = cmd.substring(6).toInt();
    motor.setSpeed(VELOCIDAD_NORMAL);
    Serial.print("SUBIENDO ");
    Serial.print(pasos);
    Serial.println(" PASOS...");
    long restantes = pasos;
    while (restantes > 0) {
      int bloque = min((long)PASOS_POR_CICLO, restantes);
      motor.step(1 * bloque);
      restantes -= bloque;
    }
    Serial.println("LISTO");
  } else {
    Serial.println("ERROR:001:Unknown command");
  }
}

void handleEmergencyStop() {
  emergencyStop = true;
  elevatorState = "EMERGENCY_STOP";
  digitalWrite(8, LOW);
  digitalWrite(9, LOW);
  digitalWrite(10, LOW);
  digitalWrite(11, LOW);
  actualizarIndicadores();
  Serial.println("OK:EMERGENCY_STOP");
}

int obtenerPasosExtras(int origen, int destino) {
  if (origen == 1 && destino == 2) return 2000;
  if (origen == 2 && destino == 3) return 2200;
  if (origen == 1 && destino == 3) return 2200;
  if (origen == 3 && destino == 2) return 1000;
  if (origen == 2 && destino == 1) return 1000;
  if (origen == 3 && destino == 1) return 1000;
  return 0;
}

bool checkEmergencyDuringMotion() {
  if (Serial.available()) {
    String incoming = Serial.readStringUntil('\n');
    incoming.trim();
    if (incoming == "EMERGENCY_STOP") {
      handleEmergencyStop();
      return true;
    } else if (incoming == "READ_STATE") {
      readState();
    }
  }
  return emergencyStop;
}

void moveToFloor(int targetFloor) {
  if (targetFloor < 1 || targetFloor > 3) {
    Serial.println("ERROR:003:Floor out of range");
    return;
  }

  if (targetFloor == currentFloor) {
    Serial.print("OK:ALREADY_AT:FLOOR_");
    Serial.println(targetFloor);
    return;
  }

  int lecturasCerrada = 0;
  for (int i = 0; i < 3; i++) {
    if (digitalRead(SENSOR_PUERTA) == SENSOR_ACTIVO) lecturasCerrada++;
    delay(50);
  }
  if (lecturasCerrada < 3) {
    Serial.println("ERROR:004:Door not closed");
    return;
  }

  int direction = (targetFloor > currentFloor) ? 1 : -1;
  elevatorState = (direction == 1) ? "GOING_UP" : "GOING_DOWN";
  actualizarIndicadores();

  Serial.print("STATUS:DIRECTION=");
  Serial.println(direction == 1 ? "UP" : "DOWN");

  emergencyStop = false;
  int pisosPorRecorrer = abs(targetFloor - currentFloor);

  for (int i = 0; i < pisosPorRecorrer; i++) {
    int pisoOrigenTramo = currentFloor;
    int pisoDestinoTramo = currentFloor + direction;
    bool esPisoFinal = (pisoDestinoTramo == targetFloor);

    // Fase 1: escape de la marca actual
    Serial.println("STATUS:ESCAPING_CURRENT_MARKER");
    motor.setSpeed(VELOCIDAD_NORMAL);

    long pasosEscape = 500;
    while (pasosEscape > 0) {
      if (checkEmergencyDuringMotion()) return;
      int bloque = min((long)PASOS_POR_CICLO, pasosEscape);
      motor.step(direction * bloque);
      pasosEscape -= bloque;
      actualizarIndicadores();
    }
    delay(100);

    ultimoEstado = digitalRead(SENSOR_PISO);
    Serial.println("STATUS:SCANNING_FOR_NEXT_MARKER");

    bool marcaEncontrada = false;
    unsigned long inicioBusqueda = millis();

    // Fase 2: buscar siguiente marca
    while (!marcaEncontrada) {
      if (checkEmergencyDuringMotion()) return;

      motor.step(direction);
      actualizarIndicadores();

      bool estadoActual = digitalRead(SENSOR_PISO);

      if (ultimoEstado == HIGH && estadoActual == LOW) {
        Serial.print("STATUS:MARKER_DETECTED:FLOOR_");
        Serial.println(pisoDestinoTramo);

        // Fase 3: pasos extras solo en piso final
        if (esPisoFinal) {
          int pasosCalculados = obtenerPasosExtras(pisoOrigenTramo, pisoDestinoTramo);

          if (pasosCalculados > 0) {
            motor.setSpeed(VELOCIDAD_NORMAL);  // ← mantener velocidad normal

            Serial.print("STATUS:APPLYING_EXTRA_STEPS=");
            Serial.println(pasosCalculados);

            // Re-sincronizar fases antes de bloques grandes
            motor.step(direction * 4);
            delay(50);

            long pasosExtrasRestantes = pasosCalculados;
            while (pasosExtrasRestantes > 0) {
              if (checkEmergencyDuringMotion()) return;
              int bloque = min((long)PASOS_POR_CICLO, pasosExtrasRestantes);
              motor.step(direction * bloque);  // ← bloques de 64, no de 1
              pasosExtrasRestantes -= bloque;
              actualizarIndicadores();
            }

            Serial.println("STATUS:EXTRA_STEPS_DONE");
          }
        }

        marcaEncontrada = true;
        break;
      }

      ultimoEstado = estadoActual;

      if (millis() - inicioBusqueda > 300000) {
        Serial.println("ERROR:007:Floor marker not found timeout");
        elevatorState = "IDLE";
        actualizarIndicadores();
        return;
      }
    }

    currentFloor = pisoDestinoTramo;

    if (!esPisoFinal) {
      Serial.print("MOVING:DIRECTION=");
      Serial.print(direction == 1 ? "UP" : "DOWN");
      Serial.print(":NOW_AT_FLOOR_");
      Serial.println(currentFloor);
    }
  }

  motor.setSpeed(VELOCIDAD_NORMAL);
  elevatorState = "IDLE";
  actualizarIndicadores();

  Serial.print("OK:ARRIVED:FLOOR_");
  Serial.println(currentFloor);
}

void openDoor() {
  if (elevatorState != "IDLE") {
    Serial.println("ERROR:006:Elevator moving");
    return;
  }

  doorState = "OPENING";
  elevatorState = "DOOR_OPENING";
  Serial.println("OK:DOOR_OPENING");

  unsigned long inicio = millis();
  while (digitalRead(SENSOR_PUERTA) == SENSOR_ACTIVO) {
    actualizarIndicadores();
    if (millis() - inicio > 5000) {
      Serial.println("ERROR:006:Timeout opening door");
      elevatorState = "IDLE";
      actualizarIndicadores();
      return;
    }
    delay(50);
  }

  doorState = "OPEN";
  elevatorState = "DOOR_OPEN";
  actualizarIndicadores();
  Serial.println("OK:DOOR_OPEN");
}

void closeDoor() {
  doorState = "CLOSING";
  elevatorState = "DOOR_CLOSING";
  Serial.println("OK:DOOR_CLOSING");

  unsigned long inicio = millis();
  while (digitalRead(SENSOR_PUERTA) != SENSOR_ACTIVO) {
    actualizarIndicadores();
    if (millis() - inicio > 5000) {
      Serial.println("ERROR:006:Timeout closing door");
      elevatorState = "IDLE";
      actualizarIndicadores();
      return;
    }
    delay(50);
  }

  doorState = "CLOSED";
  elevatorState = "IDLE";
  actualizarIndicadores();
  Serial.println("OK:DOOR_CLOSED");
}

void readState() {
  actualizarIndicadores();
  bool puertaCerrada = (digitalRead(SENSOR_PUERTA) == SENSOR_ACTIVO);
  bool enPiso = (digitalRead(SENSOR_PISO) == SENSOR_ACTIVO);

  Serial.print("STATE:FLOOR=");
  Serial.print(currentFloor);
  Serial.print(",DOOR_STATE=");
  Serial.print(doorState);
  Serial.print(",ELEVATOR_STATE=");
  Serial.print(elevatorState);
  Serial.print(",SENSOR_PUERTA=");
  Serial.print(puertaCerrada ? "CLOSED" : "OPEN");
  Serial.print(",SENSOR_PISO=");
  Serial.println(enPiso ? "AT_FLOOR" : "MOVING");
}

void reset() {
  emergencyStop = false;
  currentFloor = 1;
  doorState = "CLOSED";
  elevatorState = "IDLE";
  motor.step(0);
  actualizarIndicadores();
  Serial.println("OK:RESET_COMPLETE");
}