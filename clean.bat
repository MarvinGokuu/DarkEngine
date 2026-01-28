@echo off

echo.
echo VolcanEngine Clean
echo ==================
echo.

if exist bin (
    echo Removing bin directory...
    rd /s /q bin
    echo Done.
) else (
    echo Nothing to clean.
)

if exist dist (
    echo Removing dist directory...
    rd /s /q dist
    echo Done.
)

if exist *.log (
    echo Removing log files...
    del /q *.log
    echo Done.
)

echo.
echo Clean complete.
echo.
