$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$serverDir = Join-Path $root 'erp-server'
$webDir = Join-Path $root 'erp-web'

if (!(Test-Path $serverDir)) { throw "未找到目录: $serverDir" }
if (!(Test-Path $webDir)) { throw "未找到目录: $webDir" }

Write-Host '正在启动后端...' -ForegroundColor Cyan
Start-Process powershell -ArgumentList @(
  '-NoExit',
  '-Command',
  "Set-Location -LiteralPath '$serverDir'; mvn spring-boot:run"
) -WindowStyle Normal

Write-Host '正在启动前端...' -ForegroundColor Cyan
Start-Process powershell -ArgumentList @(
  '-NoExit',
  '-Command',
  "Set-Location -LiteralPath '$webDir'; if (!(Test-Path 'node_modules')) { npm install }; npm run dev"
) -WindowStyle Normal

Write-Host '已打开两个窗口：后端 + 前端。' -ForegroundColor Green
