# QUICK START - Smart Elevator

## Inicio Rápido

### Modo 1: Ejecutar con Maven (desarrollo)

Compilar y ejecutar en modo Simulator (por defecto):
```bash
mvn spring-boot:run
```

Compilar y ejecutar en modo Arduino (stub):
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=arduino"
```

### Modo 2: Ejecutar el JAR ya compilado

Después de compilar con `mvn package`:

**Windows:**
```bash
run.bat                    # Modo Simulator (default)
run.bat arduino            # Modo Arduino
```

**Linux/Mac:**
```bash
./run.sh                   # Modo Simulator (default)
./run.sh arduino           # Modo Arduino
```

**Directo cada compilación JAR:**
```bash
java -jar target/smart-elevator-1.0.0.jar --spring.profiles.active=simulator
```

## Compilación

```bash
# Compilar proyecto
mvn clean compile

# Compilar y generar JAR
mvn clean package

# Compilar sin ejecutar tests
mvn clean package -DskipTests

# Solo ejecutar tests
mvn test
```

## Comandos de Consola - Cheatsheet

Una vez dentro de la aplicación:

```
GO 3              → Ir al piso 3
GO 1              → Ir al piso 1
DOOR OPEN         → Abrir puerta
DOOR CLOSE        → Cerrar puerta
READ              → Ver estado actual
RESET             → Resetear a estado inicial
EXIT              → Salir
```

## Ejemplo de Sesión Completa

```
> GO 2
[OK] MOVING  | Floor: 2 | Door: CLOSED
[OK] IDLE    | Floor: 2 | Door: CLOSED

> DOOR OPEN
[OK] DOOR_OPEN | Floor: 2 | Door: OPEN

> READ
[OK] DOOR_OPEN | Floor: 2 | Door: OPEN

> DOOR CLOSE
[OK] IDLE | Floor: 2 | Door: CLOSED

> GO 5
[OK] MOVING  | Floor: 5 | Door: CLOSED
[OK] IDLE    | Floor: 5 | Door: CLOSED

> RESET
[OK] IDLE | Floor: 1 | Door: CLOSED

> EXIT
Elevador finalizado.
```

## Ver Logs

Los eventos se guardan en `elevator.log`:

```bash
# Ver últimas 10 líneas
tail -10 elevator.log

# Ver todo el archivo
cat elevator.log

# Monitorear en tiempo real (Linux/Mac)
tail -f elevator.log
```

## Estructura Rápida

- **Domain** (`domain/`): Lógica de negocio pura (sin Spring)
- **Application** (`application/`): Servicios y puertos (interfaces)
- **Infrastructure** (`infrastructure/`): Adaptadores (SimulatorAdapter, ArduinoAdapter, FileEventLogger)
- **UI** (`ui/console/`): Interfaz de usuario (ConsoleApp, CommandParser)

## Próximos Pasos

1. Implementar `ArduinoAdapter` con jSerialComm
2. Agregar REST API con Spring Web
3. Crear UI web con JavaScript/React
4. Agregar base de datos para historial de eventos

## Ayuda

Ver [README.md](README.md) para documentación completa.
