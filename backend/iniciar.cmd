@echo off
setlocal
REM Arranque de desarrollo: levanta PostgreSQL, el frontend y el backend (perfil dev).

set RAIZ=%~dp0..
set MVN=%~dp0mvnw.cmd

REM ---- Cargar variables desde .env ----
if not exist "%RAIZ%\.env" (
    echo.
    echo ERROR: no se encontro %RAIZ%\.env
    echo Copia .env.example como .env y vuelve a intentarlo:  copy .env.example .env
    echo.
    exit /b 1
)
for /f "usebackq eol=# delims== tokens=1,*" %%A in ("%RAIZ%\.env") do set "%%A=%%B"

echo.
echo Iniciando CineClub Salamanca...
echo.

REM ---- El puerto 8080 debe estar libre ----
REM Sin esta comprobacion, Spring arranca y falla al final con un stacktrace largo
REM que no dice lo obvio: que ya hay algo escuchando ahi.
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo ERROR: el puerto 8080 ya esta en uso.
    echo.
    echo Suele pasar por tener el backend levantado en Docker. Para bajarlo:
    echo     docker compose stop backend frontend
    echo.
    echo O usa directamente esa version, que ya incluye todo:  http://localhost:3000
    echo.
    exit /b 1
)

REM ---- Docker tiene que estar corriendo ----
echo [1/3] Comprobando la base de datos...
docker info >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Docker no responde. Abre Docker Desktop y espera a que arranque.
    echo.
    exit /b 1
)

REM ---- Levantar PostgreSQL ----
pushd "%RAIZ%"
docker compose up -d db >nul 2>&1
if errorlevel 1 (
    echo ERROR: no se pudo levantar el contenedor de la base de datos.
    popd
    exit /b 1
)
popd

REM Esperar a que acepte conexiones, no solo a que el contenedor exista
set INTENTOS=0
:esperar_db
docker exec cineclub-db pg_isready -U %POSTGRES_USER% -d %POSTGRES_DB% >nul 2>&1
if not errorlevel 1 goto db_lista
set /a INTENTOS+=1
if %INTENTOS% geq 30 (
    echo.
    echo ERROR: la base no acepto conexiones tras 60 segundos.
    echo Revisa el log con:  docker compose logs db
    echo.
    exit /b 1
)
timeout /t 2 /nobreak >nul
goto esperar_db

:db_lista
echo       PostgreSQL listo en el puerto %POSTGRES_PORT%.

REM ---- Frontend ----
echo [2/3] Iniciando frontend...
start "Frontend - CineClub" cmd /k "cd /d %RAIZ%\frontend && python -m http.server 3000"

REM ---- Backend ----
echo [3/3] Iniciando backend...
echo.
echo   Frontend:  http://localhost:3000
echo   Swagger:   http://localhost:8080/swagger-ui/index.html
echo   Salud:     http://localhost:8080/actuator/health
echo.
echo Ctrl+C para detener el backend. La base sigue corriendo en Docker;
echo para apagarla:  docker compose stop db
echo.

cd /d %~dp0
"%MVN%" spring-boot:run -Dspring-boot.run.profiles=dev
