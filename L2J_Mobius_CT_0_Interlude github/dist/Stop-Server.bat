@echo off
title L2 Offline Server - Stop
setlocal
echo Stopping Game and Login servers...
REM Kill java processes (the two servers). On a bundled pack these are the only Java apps;
REM if you run other Java software, close the server windows manually instead.
taskkill /IM java.exe /F >nul 2>&1

REM Cleanly shut down a BUNDLED MariaDB if present (avoids hard-killing the DB).
set "MDBIN=%~dp0mariadb\bin"
if exist "%MDBIN%\mariadb-admin.exe" (
  echo Shutting down bundled MariaDB...
  "%MDBIN%\mariadb-admin.exe" -u root --port=3306 shutdown >nul 2>&1
) else if exist "%MDBIN%\mysqladmin.exe" (
  echo Shutting down bundled MariaDB...
  "%MDBIN%\mysqladmin.exe" -u root --port=3306 shutdown >nul 2>&1
) else (
  echo (External MySQL/XAMPP left running - close its window to stop it.)
)
echo Done.
timeout /t 3 >nul
