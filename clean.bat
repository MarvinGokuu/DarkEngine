@echo off

set CLEAN_BIN=0
set CLEAN_DIST=0
set CLEAN_LOGS=0
set CLEAN_PORTABLE=0
set PORT_FREED=0
set HAS_ERRORS=0

set TMP_LOG=%TEMP%\dark_clean.log
if exist "%TMP_LOG%" del "%TMP_LOG%"

if exist bin (
    rd /s /q bin >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_BIN=1
)

if exist dist (
    rd /s /q dist >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_DIST=1
)

if exist Dark-Engine (
    rd /s /q Dark-Engine >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_PORTABLE=1
)

if exist DarkEngine.jar (
    del /q DarkEngine.jar >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_PORTABLE=1
)

if exist *.log (
    del /q *.log >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_LOGS=1
)

if exist logs (
    rd /s /q logs >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set CLEAN_LOGS=1
)

if not exist logs mkdir logs

FOR /F "tokens=5" %%a in ('netstat -aon ^| find ":13000" ^| find "LISTENING"') do (
    taskkill /F /PID %%a >>"%TMP_LOG%" 2>&1 || set HAS_ERRORS=1
    set PORT_FREED=1
)

set CLEAN_LOG_FILE=logs\clean.log

(
if %CLEAN_BIN%==1 echo Binaries removed (bin/^)
if %CLEAN_DIST%==1 echo Distribution files removed (dist/^)
if %CLEAN_LOGS%==1 echo Stale logs removed (*.log, logs/^)
if %CLEAN_PORTABLE%==1 echo Portable build removed (Dark-Engine/, DarkEngine.jar^)
if %PORT_FREED%==1 echo Network port (13000^) forcefully freed

if %CLEAN_BIN%==0 ^
if %CLEAN_DIST%==0 ^
if %CLEAN_LOGS%==0 ^
if %CLEAN_PORTABLE%==0 ^
if %PORT_FREED%==0 echo Workspace is already pristine.

if %HAS_ERRORS%==1 (
    echo Cleanup sequence incompleted
) else (
    echo Cleanup sequence completed.
)
) > "%CLEAN_LOG_FILE%"

type "%CLEAN_LOG_FILE%"

if %HAS_ERRORS%==1 (
    echo. >> "%CLEAN_LOG_FILE%"
    echo --- DETALLES DE ERROR --- >> "%CLEAN_LOG_FILE%"
    type "%TMP_LOG%" >> "%CLEAN_LOG_FILE%"
)

if exist "%TMP_LOG%" del "%TMP_LOG%"
