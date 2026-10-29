@echo off
setlocal
cd /d "%~dp0.."
echo ATENCION: se borraran los datos del contenedor PostgreSQL de desarrollo.
set /p CONFIRM=Escribe BORRAR para continuar: 
if /I not "%CONFIRM%"=="BORRAR" (
  echo Cancelado.
  pause
  exit /b 0
)
docker compose down -v
docker compose up --build -d
echo Base restablecida. Espera que el backend termine de iniciar y ejecuta Postman.
pause
