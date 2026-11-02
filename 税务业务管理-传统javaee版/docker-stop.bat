@echo off
setlocal
cd /d "%~dp0"
docker compose down
if errorlevel 1 echo Stop failed. Make sure Docker Desktop is running.
echo.
pause
