#!/usr/bin/env bash
# Script de prueba para la API REST del Elevador
# Uso: bash test-api.sh

API_BASE="http://localhost:8080/api/v1/elevators"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Smart Elevator - API Test Suite${NC}"
echo -e "${YELLOW}========================================${NC}\n"

# Test 1: Obtener estado de todos los elevadores
test_get_all_elevators() {
    echo -e "${YELLOW}[TEST 1] Obtener estado de todos los elevadores${NC}"
    curl -X GET "$API_BASE" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 2: Obtener estado de elevator-1
test_get_elevator_1() {
    echo -e "${YELLOW}[TEST 2] Obtener estado de elevator-1${NC}"
    curl -X GET "$API_BASE/elevator-1" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 3: Obtener resumen
test_get_summary() {
    echo -e "${YELLOW}[TEST 3] Obtener resumen de elevadores${NC}"
    curl -X GET "$API_BASE/status/summary" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 4: Mover elevator-1 al piso 3
test_go_to_floor() {
    echo -e "${YELLOW}[TEST 4] Mover elevator-1 al piso 3 (ASINCRÓNICO)${NC}"
    curl -X POST "$API_BASE/elevator-1/go-to-floor" \
        -H "Content-Type: application/json" \
        -d '{"targetFloor": 3}' | json_pp
    echo ""
    echo -e "${GREEN}✓ Operación asincrónica iniciada. Ver eventos en otra terminal${NC}\n"
}

# Test 5: Abrir puerta
test_open_door() {
    echo -e "${YELLOW}[TEST 5] Abrir puerta de elevator-1${NC}"
    curl -X POST "$API_BASE/elevator-1/door/open" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 6: Cerrar puerta
test_close_door() {
    echo -e "${YELLOW}[TEST 6] Cerrar puerta de elevator-1${NC}"
    curl -X POST "$API_BASE/elevator-1/door/close" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 7: Reiniciar elevator-1
test_reset() {
    echo -e "${YELLOW}[TEST 7] Reiniciar elevator-1${NC}"
    curl -X POST "$API_BASE/elevator-1/reset" \
        -H "Content-Type: application/json" | json_pp
    echo ""
}

# Test 8: Validación - Piso inválido
test_invalid_floor() {
    echo -e "${YELLOW}[TEST 8] Validación - Piso inválido (6)${NC}"
    curl -X POST "$API_BASE/elevator-1/go-to-floor" \
        -H "Content-Type: application/json" \
        -d '{"targetFloor": 6}' | json_pp
    echo ""
}

# Test 9: Multi-elevador - Crear elevator-2
test_elevator_2() {
    echo -e "${YELLOW}[TEST 9] Multi-elevador - Mover elevator-2 al piso 2${NC}"
    curl -X POST "$API_BASE/elevator-2/go-to-floor" \
        -H "Content-Type: application/json" \
        -d '{"targetFloor": 2}' | json_pp
    echo ""
}

# Test 10: SSE - Escuchar eventos
test_sse_events() {
    echo -e "${YELLOW}[TEST 10] SSE - Escuchar eventos de elevator-1${NC}"
    echo -e "${GREEN}Conectando a eventos SSE... (Presiona Ctrl+C para terminar)${NC}\n"
    curl --no-buffer -N "$API_BASE/elevator-1/events"
}

# Mostrar menú
show_menu() {
    echo -e "${YELLOW}Selecciona una prueba para ejecutar:${NC}"
    echo "  1) Get all elevators"
    echo "  2) Get elevator-1"
    echo "  3) Get summary"
    echo "  4) Go to floor (async)"
    echo "  5) Open door"
    echo "  6) Close door"
    echo "  7) Reset elevator"
    echo "  8) Invalid floor (validation)"
    echo "  9) Multi-elevator (elevator-2)"
    echo "  10) SSE Events (listen)"
    echo "  11) Run all tests"
    echo "  12) Exit"
    echo ""
    read -p "Ingresa el número: " choice
}

# Ejecutar todas las pruebas
run_all_tests() {
    echo -e "${GREEN}Ejecutando todas las pruebas...${NC}\n"
    test_get_all_elevators
    sleep 1
    test_get_elevator_1
    sleep 1
    test_get_summary
    sleep 1
    test_go_to_floor
    sleep 3 # Esperar a que la operación asincrónica complete
    test_open_door
    sleep 1
    test_close_door
    sleep 1
    test_reset
    sleep 1
    test_invalid_floor
    sleep 1
    test_elevator_2
    echo -e "${GREEN}✓ Todas las pruebas completadas${NC}\n"
}

# Verificar si curl está disponible
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl no está instalado${NC}"
    exit 1
fi

# Loop principal
while true; do
    show_menu
    
    case $choice in
        1) test_get_all_elevators ;;
        2) test_get_elevator_1 ;;
        3) test_get_summary ;;
        4) test_go_to_floor ;;
        5) test_open_door ;;
        6) test_close_door ;;
        7) test_reset ;;
        8) test_invalid_floor ;;
        9) test_elevator_2 ;;
        10) test_sse_events ;;
        11) run_all_tests ;;
        12) echo -e "${GREEN}¡Adiós!${NC}" && exit 0 ;;
        *) echo -e "${RED}Opción inválida${NC}" ;;
    esac
done
