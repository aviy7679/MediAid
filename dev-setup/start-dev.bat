@echo off
echo 🚀 Starting development environment...

REM Checking Docker
docker info >nul 2>&1
if errorlevel 1 (
echo ❌ Docker is not running. Please start Docker Desktop
pause
exit /b 1
)

echo ⚡ Starting services...
docker-compose up -d

echo ✅ Environment is up!
echo 🌐 Check your ports
echo.
echo To stop use: stop-dev.batpause