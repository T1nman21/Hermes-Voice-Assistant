@echo off
REM Launch Hermes Assistant Android emulator
REM -no-snapshot: prevents black screen from corrupted quick-boot
REM -gpu host: uses NVIDIA/Intel GPU for smooth rendering
C:\Users\Connor\AppData\Local\Android\Sdk\emulator\emulator.exe -avd hermes_voice -no-boot-anim -no-snapshot -gpu host
