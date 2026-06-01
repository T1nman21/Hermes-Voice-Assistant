@echo off
setlocal enabledelayedexpansion
title Hermes Assistant Setup

echo.
echo ============================================
echo   Hermes Assistant -- One-Click Setup
echo ============================================
echo.

set "RELAY_DIR=%~dp0relay"
set "CF_LOG=%TEMP%\cf-tunnel.log"
set "CF_URL_FILE=%TEMP%\cf-url.txt"

:: Generate token and room
set "TOKEN=%random%%random%%random%"
set "TOKEN=!TOKEN:~0,16!"
set "ROOM=HERM"

:: Kill existing Hermes relay/desktop processes (NOT the current terminal)
powershell -Command "Get-Process node -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like '*Hermes*' -or $_.MainWindowTitle -like '*Cloudflare*' } | Stop-Process -Force" 2>nul

:: ── Prerequisites ─────────────────────────────────────────────────────
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js required. Install from https://nodejs.org
    pause
    exit /b 1
)
echo [OK] Node.js found

echo [..] Installing relay dependencies...
cd /d "%RELAY_DIR%"
call npm install --silent 2>nul
cd /d "%~dp0"
echo [OK] Dependencies ready

where cloudflared >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [..] Downloading Cloudflare Tunnel...
    curl -L -s -o "%TEMP%\cloudflared.exe" "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
    if exist "%TEMP%\cloudflared.exe" (
        mkdir "%USERPROFILE%\bin" 2>nul
        move /Y "%TEMP%\cloudflared.exe" "%USERPROFILE%\bin\cloudflared.exe" >nul
        set "PATH=%USERPROFILE%\bin;%PATH%"
        echo [OK] Cloudflare Tunnel installed to %%USERPROFILE%%\bin
    ) else (
        echo [ERROR] Download failed.
        echo        Get it from https://github.com/cloudflare/cloudflared/releases
        pause
        exit /b 1
    )
) else (
    echo [OK] Cloudflare Tunnel found
)

:: ── Hermes API health check ───────────────────────────────────────────
echo [..] Checking Hermes Agent API on localhost:8642...
powershell -NoProfile -Command ^
  "try { " ^
  "  $r = Invoke-WebRequest -Uri 'http://localhost:8642/v1/models' -TimeoutSec 5 -UseBasicParsing -EA Stop; " ^
  "  if ($r.StatusCode -eq 200) { Write-Host '[OK] Hermes Agent API is running'; exit 0 } " ^
  "} catch { " ^
  "  Write-Host '[WARN] Hermes Agent API not reachable at http://localhost:8642'; " ^
  "  Write-Host '       The desktop client will retry when it comes up.'; " ^
  "  Write-Host '       Start Hermes with: hermes-agent --api-server --port 8642'; " ^
  "  exit 0 " ^
  "}"
if %ERRORLEVEL% NEQ 0 (
    echo [WARN] Could not verify Hermes API — continuing anyway
)

:: ── Phase 1: Start Cloudflare Tunnel, capture URL ─────────────────────
echo.
echo [..] Starting Cloudflare Tunnel ^(waiting for public URL...^)
del "%CF_LOG%" 2>nul
del "%CF_URL_FILE%" 2>nul

start "Cloudflare Tunnel" cmd /c "cloudflared.exe tunnel --url http://localhost:8643 2> \"%CF_LOG%\""

:: PowerShell: poll log file for trycloudflare.com URL, write to CF_URL_FILE
powershell -NoProfile -Command ^
  "$logFile = \"$env:TEMP\cf-tunnel.log\"; " ^
  "$urlFile = \"$env:TEMP\cf-url.txt\"; " ^
  "Write-Host '[..] Waiting for Cloudflare to assign a tunnel URL...'; " ^
  "for ($i = 0; $i -lt 40; $i++) { " ^
  "  if (Test-Path $logFile) { " ^
  "    $c = Get-Content $logFile -Raw -EA 0; " ^
  "    if ($c -match 'https://[a-zA-Z0-9][-a-zA-Z0-9]*\.[-a-zA-Z0-9]*\.trycloudflare\.com') { " ^
  "      $url = $matches[0]; " ^
  "      Write-Host \"[OK] Tunnel URL: $url\"; " ^
  "      $url | Out-File $urlFile -NoNewline -Encoding ASCII; " ^
  "      exit 0 " ^
  "    } " ^
  "  } " ^
  "  Start-Sleep 1.5 " ^
  "} " ^
  "Write-Host '[WARN] Timed out waiting for tunnel URL — continuing in local mode'; " ^
  "exit 1"

set "TUNNEL_URL="
set "TUNNEL_OK=0"
if exist "%CF_URL_FILE%" (
    set /p TUNNEL_URL=<"%CF_URL_FILE%"
    set "TUNNEL_OK=1"
    echo [OK] Captured tunnel URL: !TUNNEL_URL!
) else (
    echo [INFO] No tunnel URL — phone must be on same Wi-Fi to connect
)

:: ── Phase 2: Start relay server ───────────────────────────────────────
echo.
echo [..] Starting relay server on port 8643...

set "RELAY_ARGS=--port 8643 --token %TOKEN% --room %ROOM%"
if "!TUNNEL_OK!"=="1" (
    set "WSS_URL=!TUNNEL_URL:https=wss!"
    set "RELAY_ARGS=!RELAY_ARGS! --tunnel-url !WSS_URL!"
)

start "Hermes Relay" cmd /c "node \"%RELAY_DIR%\relay-server.js\" %RELAY_ARGS% && pause"
timeout /t 2 >nul

:: ── Phase 3: Start desktop client ─────────────────────────────────────
echo [..] Starting desktop client ^(bridges relay to Hermes API^)...
start "Hermes Desktop" cmd /c "node \"%RELAY_DIR%\desktop-client.js\" --room %ROOM% --token %TOKEN%"

:: ── Phase 4: Summary ──────────────────────────────────────────────────
echo.
echo ============================================
echo   READY
echo ============================================
echo   Room:    %ROOM%
echo   Secret:  %TOKEN%
if "!TUNNEL_OK!"=="1" (
    echo   Tunnel:  !TUNNEL_URL!
    echo.
    echo   Scan the QR code in the Relay window
    echo   Works from ANYWHERE — no same-network needed
) else (
    echo.
    echo   No tunnel URL — same Wi-Fi only
    echo   Make sure your phone is on the same network
    echo   and scan the QR in the Relay window
)
echo ============================================
echo.
pause
