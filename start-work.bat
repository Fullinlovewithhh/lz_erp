@echo off
setlocal EnableExtensions EnableDelayedExpansion
title start-work

set "EXPECTED_REMOTE=https://github.com/Fullinlovewithhh/Lonzvine_Fac_erp.git"

echo ======================================
echo Start Work
echo ======================================

git rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Not a git repository.
  pause
  exit /b 1
)

if exist ".git\rebase-merge" (
  echo [ERROR] Rebase in progress. Run git rebase --continue or --abort.
  pause
  exit /b 1
)
if exist ".git\rebase-apply" (
  echo [ERROR] Rebase in progress. Run git rebase --continue or --abort.
  pause
  exit /b 1
)

for /f "delims=" %%i in ('git remote get-url origin 2^>nul') do set "ORIGIN_URL=%%i"
if not defined ORIGIN_URL (
  echo [ERROR] origin not set.
  echo git remote add origin %EXPECTED_REMOTE%
  pause
  exit /b 1
)
if /i not "%ORIGIN_URL%"=="%EXPECTED_REMOTE%" (
  echo [ERROR] origin URL mismatch.
  echo current: %ORIGIN_URL%
  echo expect : %EXPECTED_REMOTE%
  pause
  exit /b 1
)

for /f %%i in ('git status --porcelain ^| find /c /v ""') do set CHANGES=%%i
if not "%CHANGES%"=="0" (
  echo [WARN] Uncommitted changes detected.
  git status --short
  echo.
  set /p AUTO_SAVE=Auto create WIP branch and save now? y or n: 
  if /i "!AUTO_SAVE!"=="y" (
    for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set TS=%%t
    set "WIP_BRANCH=feature/wip-!TS!"
    echo [INFO] Create branch: !WIP_BRANCH!
    git checkout -b "!WIP_BRANCH!"
    if errorlevel 1 (
      echo [ERROR] Cannot create WIP branch.
      pause
      exit /b 1
    )
    git add .
    if errorlevel 1 (
      echo [ERROR] git add failed.
      pause
      exit /b 1
    )
    git commit -m "WIP: auto save before start-work"
    if errorlevel 1 (
      echo [ERROR] git commit failed.
      pause
      exit /b 1
    )
    git push -u origin "!WIP_BRANCH!"
    if errorlevel 1 (
      echo [ERROR] git push failed.
      pause
      exit /b 1
    )
    echo [OK] WIP saved on !WIP_BRANCH!
    git checkout main >nul 2>nul
    if errorlevel 1 (
      echo [ERROR] Cannot checkout main after WIP save.
      pause
      exit /b 1
    )
  ) else (
    echo [INFO] Please save changes first, then run start-work again.
    pause
    exit /b 1
  )
)

git checkout main >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Cannot checkout main.
  pause
  exit /b 1
)

git pull --rebase origin main
if errorlevel 1 (
  echo [ERROR] pull --rebase failed. Conflicts:
  git diff --name-only --diff-filter=U
  pause
  exit /b 1
)

set /p TASK_NAME=Task name (example order-log-fix): 
if "%TASK_NAME%"=="" (
  echo [ERROR] Task name required.
  pause
  exit /b 1
)
set "TASK_NAME=%TASK_NAME: =-%"
set "BRANCH_NAME=feature/%TASK_NAME%"

git show-ref --verify --quiet refs/heads/%BRANCH_NAME%
if errorlevel 1 (
  git checkout -b "%BRANCH_NAME%"
) else (
  git checkout "%BRANCH_NAME%"
)
if errorlevel 1 (
  echo [ERROR] Cannot switch to %BRANCH_NAME%.
  pause
  exit /b 1
)

echo.
echo [OK] Current branch:
git branch --show-current
git status
echo.
pause
exit /b 0

