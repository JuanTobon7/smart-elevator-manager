@echo off
REM Script de prueba para la API REST del Elevador (Windows)
REM Uso: test-api.bat

setlocal enabledelayedexpansion

set API_BASE=http://localhost:8080/api/v1/elevators

echo.
echo ========================================
echo Smart Elevator - API Test Suite (Windows)
echo ========================================
echo.

:menu
cls
echo.
echo ========================================
echo Smart Elevator - API Test Suite
echo ========================================
echo.
echo Selecciona una prueba para ejecutar:
echo   1) Get all elevators
echo   2) Get elevator-1
echo   3) Get summary
echo   4) Go to floor (async)
echo   5) Open door
echo   6) Close door
echo   7) Reset elevator
echo   8) Invalid floor (validation)
echo   9) Multi-elevator (elevator-2)
echo   10) Exit
echo.
set /p choice="Ingresa el numero: "

if "%choice%"=="1" goto test_all
if "%choice%"=="2" goto test_elevator_1
if "%choice%"=="3" goto test_summary
if "%choice%"=="4" goto test_go_to_floor
if "%choice%"=="5" goto test_open_door
if "%choice%"=="6" goto test_close_door
if "%choice%"=="7" goto test_reset
if "%choice%"=="8" goto test_invalid
if "%choice%"=="9" goto test_elevator_2
if "%choice%"=="10" goto end
echo.
echo ERROR: Opcion invalida
timeout /t 2
goto menu

:test_all
echo.
echo [TEST] Obtener estado de todos los elevadores
curl -X GET "%API_BASE%" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_elevator_1
echo.
echo [TEST] Obtener estado de elevator-1
curl -X GET "%API_BASE%/elevator-1" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_summary
echo.
echo [TEST] Obtener resumen
curl -X GET "%API_BASE%/status/summary" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_go_to_floor
echo.
echo [TEST] Mover elevator-1 al piso 3
curl -X POST "%API_BASE%/elevator-1/go-to-floor" ^
  -H "Content-Type: application/json" ^
  -d {"targetFloor": 3}
echo.
echo [INFO] Operacion asincroncia iniciada. Ver eventos en otra terminal
echo.
pause
goto menu

:test_open_door
echo.
echo [TEST] Abrir puerta
curl -X POST "%API_BASE%/elevator-1/door/open" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_close_door
echo.
echo [TEST] Cerrar puerta
curl -X POST "%API_BASE%/elevator-1/door/close" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_reset
echo.
echo [TEST] Reiniciar elevator-1
curl -X POST "%API_BASE%/elevator-1/reset" ^
  -H "Content-Type: application/json"
echo.
pause
goto menu

:test_invalid
echo.
echo [TEST] Validacion - Piso invalido
curl -X POST "%API_BASE%/elevator-1/go-to-floor" ^
  -H "Content-Type: application/json" ^
  -d {"targetFloor": 6}
echo.
pause
goto menu

:test_elevator_2
echo.
echo [TEST] Multi-elevador - Mover elevator-2 al piso 2
curl -X POST "%API_BASE%/elevator-2/go-to-floor" ^
  -H "Content-Type: application/json" ^
  -d {"targetFloor": 2}
echo.
pause
goto menu

:end
echo.
echo [INFO] Saliendo...
exit /b 0
