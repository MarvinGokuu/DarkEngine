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

:: Auto-discover all Java files to prevent missing dependencies or OS command-line limits
dir /s /B src\*.java > compile_list.txt

javac -d bin --enable-preview --source %JAVA_MAJOR% --add-modules jdk.incubator.vector,jdk.httpserver -Xlint:-incubating -cp src ^
         -J-XX:+UseZGC -J-Xms4G -J-Xmx4G -J-XX:+AlwaysPreTouch ^
         @compile_list.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    exit /b %errorlevel%
)

:: Copy resources (images, configurations, etc.) to bin
if not exist bin\sv\dark\ui mkdir bin\sv\dark\ui
copy /y src\sv\dark\ui\darkengine_logo.png bin\sv\dark\ui\darkengine_logo.png >nul

if not exist bin\sv\dark\admin mkdir bin\sv\dark\admin
copy /y src\sv\dark\admin\editor.html bin\sv\dark\admin\editor.html >nul
copy /y src\sv\dark\admin\index.html bin\sv\dark\admin\index.html >nul

echo [SUCCESS] Build complete. Binaries in: bin\

:: 3. Package as JAR
echo [STAGE] Creating DarkEngine.jar...
jar --create --file DarkEngine.jar --main-class sv.dark.state.DarkEngineMaster -C bin .

if %errorlevel% neq 0 (
    echo [ERROR] JAR creation failed.
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
    exit /b %errorlevel%
)

:: 5. Copy Native Libraries
echo [STAGE] Bundling Native Libraries (DLLs)...
if not exist Dark-Engine\lib mkdir Dark-Engine\lib
copy /y lib\glfw3.dll Dark-Engine\lib\glfw3.dll >nul
copy /y lib\soft_oal.dll Dark-Engine\lib\soft_oal.dll >nul

echo --------------------------------------------
echo [SUCCESS] Dark-Engine App Image ready.
echo Portable Folder: Dark-Engine\
echo Run: Dark-Engine\Dark-Engine.exe
echo Log: darkengine.log (en el directorio de trabajo)
echo --------------------------------------------