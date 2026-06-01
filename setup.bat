@echo off
setlocal enabledelayedexpansion
title Hermes Assistant Setup

echo.
echo ============================================
echo   Hermes Assistant — One-Click Setup
echo ============================================
echo.

set RELAY_DIR=%~dp0relay

:: Generate a random shared secret
set TOKEN=%random%%random%%random%
set TOKEN=!TOKEN:~0,16!
set ROOM=HERM

:: Check Node.js
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js required. Install from https://nodejs.org
    pause
    exit /b 1
)
echo [OK] Node.js found

:: Install deps
echo [..] Installing dependencies...
cd /d "%RELAY_DIR%"
call npm install --silent 2>nul || call npm install 2>&1
cd /d "%~dp0"
echo [OK] Dependencies ready

:: Check cloudflared
where cloudflared >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [..] Downloading Cloudflare Tunnel...
    curl -L -s -o "%TEMP%\cloudflared.exe" "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
    if exist "%TEMP%\cloudflared.exe" (
        mkdir "%USERPROFILE%\bin" 2>nul
        move "%TEMP%\cloudflared.exe" "%USERPROFILE%\bin\cloudflared.exe" >nul
        set "PATH=%USERPROFILE%\bin;%PATH%"
        echo [OK] Cloudflare Tunnel installed
    ) else (
        echo [WARN] Auto-download failed. Get it from:
        echo        https://github.com/cloudflare/cloudflared/releases
    )
) else (
    echo [OK] Cloudflare Tunnel found
)

echo.
echo ============================================
echo   CONNECTION DETAILS
echo ============================================
echo   Room code: %ROOM%
echo   Secret:    %TOKEN%
echo ============================================
echo.

:: Start relay with token
start "Hermes Relay" cmd /c "node %RELAY_DIR%\relay-server.js --port 8643 --token %TOKEN% && pause"
timeout /t 2 >nul

:: Start cloudflared
start "Cloudflare Tunnel" cmd /c "cloudflared.exe tunnel --url http://localhost:8643"
timeout /t 2 >nul

:: Start desktop client with same token
start "Hermes Desktop" cmd /c "node %RELAY_DIR%\desktop-client.js --room %ROOM% --token %TOKEN%"

echo.
echo ============================================
echo   READY — Open the app on your phone
echo ============================================
echo.
echo   Scan the QR code in the relay window
echo   (local network pairing, no typing needed)
echo.
echo   OR for remote access:
echo   Copy the URL from the Cloudflare Tunnel window
echo   Room: %ROOM%   Secret: %TOKEN%
echo ============================================
echo.
pause
