@echo off
title Dark-Engine Build System
cls

echo [SYSTEM] Starting Dark-Engine Build v2.3-mvp
echo --------------------------------------------

:: 1. Clean
if exist bin rd /s /q bin
if exist Dark-Engine rd /s /q Dark-Engine
mkdir bin

:: Detectar version del javac activo
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m
echo [INFO] javac: %JAVAC_VER%

:: 2. Compile
echo [STAGE] Compiling Source...
javac -d bin --enable-preview --source %JAVA_MAJOR% --add-modules jdk.incubator.vector,jdk.httpserver -Xlint:-incubating -cp src ^
         -J-XX:+UseZGC -J-Xms4G -J-Xmx4G -J-XX:+AlwaysPreTouch ^
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
    echo [ERROR] Compilation failed.
    pause
    exit /b %errorlevel%
)

:: Copy resources (images, configurations, etc.) to bin
if not exist bin\sv\dark\ui mkdir bin\sv\dark\ui
copy /y src\sv\dark\ui\darkengine_logo.png bin\sv\dark\ui\darkengine_logo.png >nul

echo [SUCCESS] Build complete. Binaries in: bin\

:: 3. Package as JAR
echo [STAGE] Creating DarkEngine.jar...
jar --create --file DarkEngine.jar --main-class sv.dark.state.DarkEngineMaster -C bin .

if %errorlevel% neq 0 (
    echo [ERROR] JAR creation failed.
    pause
    exit /b %errorlevel%
)

:: 4. Native App Image — GUI subsystem (no console window)
echo [STAGE] Generating Native App Image...
jpackage --name "Dark-Engine" ^
         --input . ^
         --main-jar DarkEngine.jar ^
         --type app-image ^
         --add-modules jdk.incubator.vector,java.base,jdk.httpserver,jdk.management,java.desktop ^
         --java-options "--enable-preview --enable-native-access=ALL-UNNAMED -XX:+UseZGC -Xms4G -Xmx4G -XX:+AlwaysPreTouch" ^
         --arguments "--enable-preview"

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed.
    pause
    exit /b %errorlevel%
)

echo --------------------------------------------
echo [SUCCESS] Dark-Engine App Image ready.
echo Run: Dark-Engine\Dark-Engine.exe
echo Log: darkengine.log (en el directorio de trabajo)
echo --------------------------------------------
pause