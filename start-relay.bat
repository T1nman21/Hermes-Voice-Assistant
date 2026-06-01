@echo off
REM ============================================
REM  Hermes Assistant — Start Relay + Tunnel
REM ============================================

setlocal
set RELAY_DIR=%~dp0relay

echo ============================================
echo  Hermes Assistant Relay
echo ============================================

REM Kill any existing relay or tunnel
taskkill /f /im node.exe /fi "WINDOWTITLE eq Hermes*" 2>nul

echo Starting WebSocket relay on port 8643...
start "Hermes Relay" /min cmd /c "node %RELAY_DIR%\relay-server.js --port 8643"
timeout /t 2 >nul

echo.
echo Starting Cloudflare Tunnel...
echo.
echo ============================================
echo  A new window will open.
echo  Copy the trycloudflare.com URL from it.
echo  Then paste it into the Hermes Assistant app.
echo ============================================

start "Cloudflare Tunnel" cmd /c "cloudflared.exe tunnel --url http://localhost:8643"

echo.
echo The shared secret (token) is printed in the
echo relay window (Hermes Relay). You'll need it too.
echo.
pause
