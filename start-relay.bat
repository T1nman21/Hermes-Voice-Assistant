@echo off
REM ============================================
REM  Hermes Assistant — Start Relay + Tunnel
REM  Launches the WebSocket relay + Cloudflare
REM  Tunnel so your phone can connect from
REM  anywhere on the internet.
REM ============================================

setlocal enabledelayedexpansion

set RELAY_PORT=8643
set RELAY_DIR=%~dp0relay

echo ============================================
echo  Hermes Assistant Relay
echo ============================================
echo.

REM Kill any existing relay or tunnel
taskkill /f /im node.exe /fi "WINDOWTITLE eq Hermes*" 2>nul

echo [1/3] Starting WebSocket relay on port %RELAY_PORT%...
start "Hermes Relay" /min cmd /c "node %RELAY_DIR%\relay-server.js --port %RELAY_PORT%"
timeout /t 2 >nul

echo [2/3] Starting Cloudflare Tunnel...
echo         (first time may take 10-15 seconds)
echo.

REM Start cloudflared and capture the URL
for /f "tokens=*" %%a in ('cloudflared.exe tunnel --url http://localhost:%RELAY_PORT% 2^>^&1 ^| findstr "trycloudflare.com"') do (
    set TUNNEL_URL=%%a
)

REM The above blocks, so we use a temp file approach instead
start "Hermes Tunnel" /min cmd /c "cloudflared.exe tunnel --url http://localhost:%RELAY_PORT% > %TEMP%\hermes_tunnel.log 2>&1"

echo [3/3] Waiting for tunnel URL...
timeout /t 8 >nul

REM Extract the URL from the log
for /f "tokens=*" %%a in ('findstr "trycloudflare.com" %TEMP%\hermes_tunnel.log 2^>nul') do (
    set LINE=%%a
)

REM Parse the URL out of the line
for /f "tokens=2 delims= " %%u in ("!LINE!") do set TUNNEL_URL=%%u

if "!TUNNEL_URL!"=="" (
    echo.
    echo WARNING: Could not extract tunnel URL.
    echo Check %TEMP%\hermes_tunnel.log for the URL.
    echo.
    echo Fallback: use ws://localhost:%RELAY_PORT% (local only)
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo  TUNNEL READY
echo ============================================
echo.
echo  Public URL: !TUNNEL_URL!
echo.
echo  Room code:   HERM
echo.
echo  In the Hermes Assistant app, enter:
echo    Desktop: !TUNNEL_URL:https://=!
echo    Room:     HERM
echo.
echo  Press Ctrl+C in this window to stop.
echo ============================================
echo.

REM Keep window open
pause >nul
