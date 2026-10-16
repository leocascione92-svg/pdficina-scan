@echo off
setlocal
cd /d "%~dp0"
title PDFicina Scan - Build APK Android
where powershell.exe >nul 2>nul
if errorlevel 1 (
  echo PowerShell non disponibile. Richiesto Windows 10/11.
  pause
  exit /b 1
)
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\BUILD_APK.ps1"
if errorlevel 1 (
  echo.
  echo Compilazione non riuscita. Controlla compilazione.log, se presente.
  pause
  exit /b 1
)
echo.
echo Fatto. APK in questa cartella.
pause
