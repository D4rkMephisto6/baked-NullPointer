@echo off
setlocal
cd /d "%~dp0.."
echo === NullPointer POS - Sprint 1 ===
docker --version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Docker no esta instalado o Docker Desktop no esta iniciado.
  echo Instala/abre Docker Desktop y vuelve a ejecutar este archivo.
  pause
  exit /b 1
)
docker compose up --build -d
if errorlevel 1 (
  echo ERROR: no se pudo iniciar el proyecto. Revisa el mensaje anterior.
  pause
  exit /b 1
)
echo.
echo Backend: http://localhost:8080/api/v1/health
echo Swagger no esta agregado; usa la coleccion Postman de la carpeta postman.
echo Logs: docker compose logs -f backend
pause
