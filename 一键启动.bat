@echo off
setlocal
chcp 65001 >nul
set ROOT=%~dp0
powershell -ExecutionPolicy Bypass -File "%ROOT%start-all.ps1"
endlocal
