@echo off
cd /d "%~dp0"
setlocal

set LOG_FILE=aaa_test_report.log
set TMP_LOG=test_temp.log

echo ==============================================
echo  DARK ENGINE - AAA+ TEST SUITE EXECUTOR
echo ==============================================
echo.

echo [TEST] Compiling dependencies...
call build.bat
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed. Cannot run tests.
    type compile.log
    exit /b 1
)

echo ============================================== > %LOG_FILE%
echo  DARK ENGINE - AAA+ TEST SUITE EXECUTOR >> %LOG_FILE%
echo ============================================== >> %LOG_FILE%
echo. >> %LOG_FILE%

echo.
echo [TEST] Running tests...
echo.

set JAVA_CMD=java --enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector

call :run_test "1/17" "Bus Benchmark" "sv.dark.test.BusBenchmarkTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "2/17" "Bus Coordination" "sv.dark.bus.BusCoordinationTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "3/17" "Bus Hardware" "sv.dark.bus.BusHardwareTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "4/17" "Ultra Fast Boot" "sv.dark.test.UltraFastBootTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "5/17" "Graceful Shutdown" "sv.dark.test.GracefulShutdownTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "6/17" "Power Saving" "sv.dark.test.PowerSavingTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "7/17" "Governor Telemetry Validation" "sv.dark.test.GovernorTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "8/17" "Particle System Determinism" "sv.dark.test.ParticleSystemDeterminismTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "9/17" "System Registry Capacity" "sv.dark.test.SystemRegistryCapacityTest" "--add-opens java.base/java.util=ALL-UNNAMED"
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "10/17" "Dependency Graph Performance" "sv.dark.test.DependencyGraphPerformanceTest" "--add-opens java.base/java.util=ALL-UNNAMED"
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "11/17" "Metrics Aggregation" "sv.dark.test.MetricsAggregationTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "12/17" "System State Manager" "sv.dark.test.SystemStateManagerTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "13/17" "Bus Benchmark (Final Validation)" "sv.dark.test.BusBenchmarkTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "14/17" "SIMD Data Accelerator Throughput" "sv.dark.core.DarkDataAccelerator" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "15/17" "SIMD Physics Engine Throughput" "sv.dark.test.SimdPhysicsDemoTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "16/17" "SIMD Kinematics Throughput" "sv.dark.test.SystemSIMDKinematicsTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "17/19" "GPU Compute Culling" "sv.dark.test.SystemGPUCullingTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "18/19" "VRAM Lifecycle (FBO Leak Test)" "sv.dark.test.SystemVRAMLeakTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "19/21" "Broadphase Culling Benchmark" "sv.dark.test.SpatialHashGridTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "20/21" "Elastic Collision Dynamics" "sv.dark.test.ElasticCollisionTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

call :run_test "21/21" "GPU Particle Emitter Structure" "sv.dark.test.GPUParticleStressTest" ""
if %ERRORLEVEL% NEQ 0 goto :test_failed

echo.
echo ==============================================
echo  ALL AAA+ TESTS PASSED SUCCESSFULLY!
echo ==============================================

echo.
%JAVA_CMD% -cp bin sv.dark.test.SummaryGenerator

if exist %TMP_LOG% del /q %TMP_LOG%
exit /b 0

:run_test
<nul set /p="[%~1] %~2... "
echo. >> %LOG_FILE%
echo [%~1] %~2 >> %LOG_FILE%

%JAVA_CMD% %~4 -cp bin %~3 > %TMP_LOG% 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] 0 errors
    type %TMP_LOG% >> %LOG_FILE%
    exit /b 0
) else (
    echo [FAIL]
    echo.
    type %TMP_LOG%
    type %TMP_LOG% >> %LOG_FILE%
    exit /b 1
)

:test_failed
echo.
echo ==============================================
echo  [ERROR] AAA+ TEST SUITE FAILED
echo ==============================================
if exist %TMP_LOG% del /q %TMP_LOG%
exit /b 1
