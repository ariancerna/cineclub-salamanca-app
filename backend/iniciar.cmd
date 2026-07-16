@echo off
REM Cargue variables de entorno desde .env
for /f "usebackq eol=# delims== tokens=1,*" %%A in ("%~dp0..\.env") do (
    set "%%A=%%B"
    echo Cargado: %%A
)
cd /d %~dp0

set MVN=%~dp0mvnw.cmd

echo.
echo Iniciando CineClub Salamanca...
echo.
echo [1/2] Iniciando frontend...
start "Frontend - CineClub" cmd /k "cd /d %~dp0..\frontend && python -m http.server 3000"

echo [2/2] Iniciando backend...
echo.
echo Frontend en: http://localhost:3000
echo Swagger en:  http://localhost:8080/swagger-ui/index.html
echo.
echo Presiona Ctrl+C para detener el backend.
echo.

"%MVN%" spring-boot:run -Dspring-boot.run.profiles=dev
