@echo off
setlocal EnableExtensions EnableDelayedExpansion
title finish-work

echo ======================================
echo Finish Work
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
    echo [INFO] Finish canceled.
    pause
    exit /b 1
  )
)

if exist ".git\rebase-merge" (
  echo [ERROR] Rebase in progress. Resolve first.
  pause
  exit /b 1
)
if exist ".git\rebase-apply" (
  echo [ERROR] Rebase in progress. Resolve first.
  pause
  exit /b 1
)

echo [INFO] Branch: %CUR_BRANCH%
git fetch origin
if errorlevel 1 (
  echo [ERROR] git fetch failed.
  pause
  exit /b 1
)

for /f %%i in ('git rev-list --count HEAD..origin/main') do set BEHIND=%%i
if not "%BEHIND%"=="0" (
  echo [INFO] Behind origin/main by %BEHIND% commit(s). Rebase now...
  git rebase origin/main
  if errorlevel 1 (
    echo [ERROR] Rebase conflict:
    git diff --name-only --diff-filter=U
    pause
    exit /b 1
  )
)

if exist "package.json" (
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed.
    pause
    exit /b 1
  )
  call npm run build
  if errorlevel 1 (
    echo [ERROR] npm run build failed.
    pause
    exit /b 1
  )
) else (
  if exist "erp-web\package.json" (
    pushd erp-web
    call npm install
    if errorlevel 1 (
      popd
      echo [ERROR] erp-web npm install failed.
      pause
      exit /b 1
    )
    call npm run build
    if errorlevel 1 (
      popd
      echo [ERROR] erp-web npm run build failed.
      pause
      exit /b 1
    )
    popd
  )
)

if exist "pom.xml" (
  call mvn test
  if errorlevel 1 (
    echo [ERROR] mvn test failed.
    pause
    exit /b 1
  )
) else (
  if exist "erp-server\pom.xml" (
    pushd erp-server
    call mvn test
    if errorlevel 1 (
      popd
      echo [ERROR] erp-server mvn test failed.
      pause
      exit /b 1
    )
    popd
  )
)

git status
set /p FINAL_MSG=Commit message: 
if "%FINAL_MSG%"=="" (
  echo [ERROR] Commit message required.
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
) else (
  git commit -m "%FINAL_MSG%"
  if errorlevel 1 (
    echo [ERROR] git commit failed.
    pause
    exit /b 1
  )
)

git push -u origin "%CUR_BRANCH%"
if errorlevel 1 (
  echo [ERROR] git push failed.
  pause
  exit /b 1
)

echo [OK] Done. Create Pull Request on GitHub.
echo [RULE] Do NOT use git push --force.
pause
exit /b 0

