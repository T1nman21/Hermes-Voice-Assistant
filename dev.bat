@echo off
REM Hermes Voice — quick rebuild & install to emulator
REM Usage: dev.bat

setlocal
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot
set ANDROID_SDK_ROOT=C:\Users\Connor\AppData\Local\Android\Sdk

echo === Building ===
call gradlew assembleStandardDebug --no-daemon
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%

echo === Installing to emulator ===
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" install -r app\build\outputs\apk\standard\debug\hermes-voice-standard-debug.apk

echo === Launching ===
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" shell am start -n com.xnu.rocky/.MainActivity

echo === Done ===
