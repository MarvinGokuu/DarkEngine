@echo off

echo.
echo ==============================================
echo  DARK ENGINE - CLEANUP ROUTINE
echo ==============================================
echo.

set CLEAN_BIN=0
set CLEAN_DIST=0
set CLEAN_LOGS=0
set CLEAN_PORTABLE=0
set PORT_FREED=0

if exist bin (
    rd /s /q bin
    set CLEAN_BIN=1
)

if exist dist (
    rd /s /q dist
    set CLEAN_DIST=1
)

if exist Dark-Engine (
    rd /s /q Dark-Engine
    set CLEAN_PORTABLE=1
)

if exist DarkEngine.jar (
    del /q DarkEngine.jar
    set CLEAN_PORTABLE=1
)

if exist *.log (
    del /q *.log
    set CLEAN_LOGS=1
)

if exist logs (
    rd /s /q logs
    set CLEAN_LOGS=1
)

if not exist logs mkdir logs

FOR /F "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
    set PORT_FREED=1
)

set CLEAN_LOG_FILE=logs\clean.log

(
echo.
echo ==============================================
echo  DARK ENGINE - CLEANUP ROUTINE
echo ==============================================
echo.

:: SUMMARY
if %CLEAN_BIN%==1 echo [CLEAN] Binaries removed (bin/)
if %CLEAN_DIST%==1 echo [CLEAN] Distribution files removed (dist/)
if %CLEAN_LOGS%==1 echo [CLEAN] Stale logs removed (*.log, logs/)
if %CLEAN_PORTABLE%==1 echo [CLEAN] Portable build removed (Dark-Engine/, DarkEngine.jar)
if %PORT_FREED%==1 echo [CLEAN] Network port (8080) forcefully freed

if %CLEAN_BIN%==0 if %CLEAN_DIST%==0 if %CLEAN_LOGS%==0 if %CLEAN_PORTABLE%==0 if %PORT_FREED%==0 echo [CLEAN] Workspace is already pristine.

echo.
echo [SUCCESS] Cleanup sequence completed.
echo.
) > %CLEAN_LOG_FILE%

type %CLEAN_LOG_FILE%
