@echo off
set MVN=%~dp0mvnw.cmd

echo Ejecutando 22 pruebas unitarias...
echo.

"%MVN%" test

echo.
echo Presiona una tecla para salir...
pause > nul
