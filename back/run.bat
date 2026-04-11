@echo off
REM Script para ejecutar el Smart Elevator en Windows
REM Uso: run.bat [perfil]
REM Perfiles: simulator (default), arduino

setlocal enabledelayedexpansion

if "%1"=="" (
    set PROFILE=simulator
) else (
    set PROFILE=%1
)

echo =====================================================
echo   Smart Elevator - Iniciando...
echo   Profile: !PROFILE!
echo =====================================================
echo.

java -jar "target\smart-elevator-1.0.0.jar" --spring.profiles.active=!PROFILE!

endlocal
