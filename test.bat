@echo off
setlocal

echo.
echo VolcanEngine Test Suite
echo ========================
echo.

if not exist bin (
    echo Error: Project not compiled. Run build.bat first.
    exit /b 1
)

echo Running tests...
echo.

:: Bus tests
echo [1/7] Bus Benchmark...
java -cp bin sv.volcan.bus.BusBenchmarkTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo [2/7] Bus Coordination...
java -cp bin sv.volcan.bus.BusCoordinationTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo [3/7] Bus Hardware...
java -cp bin sv.volcan.bus.BusHardwareTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

:: System tests
echo.
echo [4/7] Ultra Fast Boot...
java -cp bin sv.volcan.test.UltraFastBootTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo [5/7] Graceful Shutdown...
java -cp bin sv.volcan.test.GracefulShutdownTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo [6/7] Power Saving...
java -cp bin sv.volcan.test.PowerSavingTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo [7/7] Bus Benchmark (final)...
java -cp bin sv.volcan.bus.BusBenchmarkTest
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo ========================
echo All tests passed.
echo ========================
echo.
endlocal
exit /b 0

:test_failed
echo.
echo ========================
echo Test failed.
echo ========================
echo.
endlocal
exit /b 1
