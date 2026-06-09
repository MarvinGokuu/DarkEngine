@echo off
title Dark-Engine Compiler
cls

echo.
echo Dark-Engine Build System
echo ========================
echo.

:: Detectar version del javac activo (funciona con cualquier JDK 23+)
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m
echo [INFO] javac detectado: %JAVAC_VER% (major: %JAVA_MAJOR%)

:: Clean previous build
if exist bin rd /s /q bin
mkdir bin

:: Compile con la version detectada dinamicamente
echo [STAGE] Compiling...
javac -d bin --enable-preview --source %JAVA_MAJOR% ^
    --add-modules jdk.incubator.vector ^
    -Xlint:-incubating ^
    -cp src ^
    -J-XX:+UseZGC ^
    -J-Xms4G -J-Xmx4G ^
    -J-XX:+AlwaysPreTouch ^
    src\sv\dark\state\DarkEngineMaster.java ^
    src\sv\dark\kernel\*.java ^
    src\sv\dark\core\*.java ^
    src\sv\dark\core\memory\*.java ^
    src\sv\dark\core\systems\*.java ^
    src\sv\dark\state\*.java ^
    src\sv\dark\bus\*.java ^
    src\sv\dark\net\*.java ^
    src\sv\dark\test\*.java ^
    src\sv\dark\ui\*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed. Check errors above.
    exit /b %errorlevel%
)

:: Copy resources (images, configurations, etc.) to bin
if not exist bin\sv\dark\ui mkdir bin\sv\dark\ui
copy /y src\sv\dark\ui\darkengine_logo.png bin\sv\dark\ui\darkengine_logo.png >nul

echo [SUCCESS] Build complete.
echo.

:: Run engine sin consola visible
echo [STAGE] Starting Dark-Engine...
start "Dark-Engine" javaw --enable-preview --enable-native-access=ALL-UNNAMED ^
    --add-modules jdk.incubator.vector ^
    -cp bin sv.dark.state.DarkEngineMaster

pause
