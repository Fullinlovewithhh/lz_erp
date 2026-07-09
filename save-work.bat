@echo off
setlocal EnableExtensions EnableDelayedExpansion
title save-work

echo ======================================
echo Save Work
echo ======================================

git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Not a git repository.
  pause
  exit /b 1
)

for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set "CUR_BRANCH=%%i"
if /i "%CUR_BRANCH%"=="main" (
  echo [WARN] Current branch is main.
  set /p AUTO_SWITCH=Auto create/switch WIP feature branch now? y or n: 
  if /i "!AUTO_SWITCH!"=="y" (
    for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set TS=%%t
    set "WIP_BRANCH=feature/wip-!TS!"
    git checkout -b "!WIP_BRANCH!" >nul 2>nul
    if errorlevel 1 (
      git checkout "!WIP_BRANCH!" >nul 2>nul
      if errorlevel 1 (
        echo [ERROR] Cannot create/switch WIP branch.
        pause
        exit /b 1
      )
    )
    for /f "delims=" %%i in ('git rev-parse --abbrev-ref HEAD') do set "CUR_BRANCH=%%i"
    echo [INFO] Switched to !CUR_BRANCH!
  ) else (
    echo [INFO] Save canceled.
    pause
    exit /b 1
  )
)

echo [INFO] Branch: %CUR_BRANCH%
git status

for /f %%i in ('git status --porcelain ^| find /c /v ""') do set CHANGES=%%i
if "%CHANGES%"=="0" (
  echo [INFO] No changes to save.
  pause
  exit /b 0
)

set /p WIP_MSG=Save note: 
if "%WIP_MSG%"=="" (
  echo [ERROR] Save note required.
  pause
  exit /b 1
)

git add .
if errorlevel 1 (
  echo [ERROR] git add failed.
  pause
  exit /b 1
)

git diff --cached --quiet
if %errorlevel%==0 (
  echo [INFO] No staged changes.
  pause
  exit /b 0
)

git commit -m "WIP: %WIP_MSG%"
if errorlevel 1 (
  echo [ERROR] git commit failed.
  pause
  exit /b 1
)

git push -u origin "%CUR_BRANCH%"
if errorlevel 1 (
  echo [ERROR] git push failed.
  pause
  exit /b 1
)

echo [OK] Saved and pushed.
pause
exit /b 0

