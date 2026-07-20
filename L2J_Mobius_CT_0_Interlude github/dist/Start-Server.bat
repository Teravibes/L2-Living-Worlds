@echo off
title L2 Offline Server - Launcher
REM One-click boot for the whole stack. Edit launcher\launcher.ini for your machine.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0launcher\launcher.ps1"
echo.
pause
