@echo off
REM Build APK and create release package
setlocal
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot

echo Building APK...
call gradlew assembleDebug --no-daemon
if %ERRORLEVEL% NEQ 0 exit /b 1

echo.
echo Creating release package...
set PKG=hermes-assistant-release
rmdir /s /q %PKG% 2>nul
mkdir %PKG%

copy setup.bat %PKG%\
xcopy /e /i relay %PKG%\relay\ >nul
copy app\build\outputs\apk\debug\hermes-assistant-debug.apk %PKG%\hermes-assistant.apk

:: Remove node_modules from package (user runs npm install)
rmdir /s /q %PKG%\relay\node_modules 2>nul

echo.
echo ============================================
echo   Package created: %PKG%\
echo ============================================
echo   Files:
dir /b %PKG%
echo.
echo   Send this folder to users.
echo   They run: setup.bat
echo   Then install the APK on their phone.
echo ============================================
