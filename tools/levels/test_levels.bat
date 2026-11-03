@echo off
cd /d "%~dp0\..\.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "tools\levels\test_levels.ps1" %*
pause
