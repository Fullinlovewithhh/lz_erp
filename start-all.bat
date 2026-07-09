@echo off
setlocal
chcp 65001 >nul
set ROOT=%~dp0
start "后端-ERP" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%erp-server'; mvn spring-boot:run"
start "前端-ERP" powershell -NoExit -Command "Set-Location -LiteralPath '%ROOT%erp-web'; if (!(Test-Path 'node_modules')) { npm install }; npm run dev"
endlocal
