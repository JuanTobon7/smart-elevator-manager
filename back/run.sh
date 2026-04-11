#!/bin/bash
# Script para ejecutar el Smart Elevator en Linux/Mac
# Uso: ./run.sh [perfil]
# Perfiles: simulator (default), arduino

PROFILE="${1:-simulator}"

echo "====================================================="
echo "  Smart Elevator - Iniciando..."
echo "  Profile: $PROFILE"
echo "====================================================="
echo

java -jar "target/smart-elevator-1.0.0.jar" --spring.profiles.active="$PROFILE"
