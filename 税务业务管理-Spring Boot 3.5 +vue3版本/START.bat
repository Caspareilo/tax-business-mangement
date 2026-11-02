@echo off
setlocal
cd /d "%~dp0"
title Tax Business System - Docker Start
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-docker.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" echo Startup failed. Read the error above.
echo.
pause
exit /b %EXIT_CODE%
