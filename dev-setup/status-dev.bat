@echo off
echo 📊 Service status:
echo.

docker-compose ps

echo.
echo 💻 Resource consumption:
docker stats --no-stream

echo.
echo To update, run this file again
pause