@echo off
title Build One-Click Pack
REM Assembles L2J-Offline-OneClick.zip (server + bundled JDK + bundled MariaDB + launcher).
REM Run this on your dev/live PC (needs JDK 25 + Ant). See build-pack.ps1 for options.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-pack.ps1" %*
echo.
pause
