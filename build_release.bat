@echo off
title Dark-Engine Release Compiler V1.0
cls

echo [SYSTEM] Starting Dark-Engine Production Build V1.0
echo ---------------------------------------------------

:: 1. Clean
if exist bin rd /s /q bin
if exist Dark-Engine-V1.0 rd /s /q Dark-Engine-V1.0
if exist DarkEngine-v1.0.jar del /q DarkEngine-v1.0.jar
mkdir bin

:: Detect javac version
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m
echo [INFO] Compiler: JDK %JAVAC_VER% (Aggressive Zero-Debug Mode)

:: 2. Discover Source Files (excluding tests)
echo [STAGE] Discovering source files (Excluding test packages)...
dir /s /B src\*.java | findstr /v "\\test\\" | findstr /v "\\benchmark\\" > compile_release_list.txt

:: 3. Compile
echo [STAGE] Compiling Source without debug symbols (-g:none)...
javac -d bin -g:none --enable-preview --source %JAVA_MAJOR% ^
      --add-modules jdk.incubator.vector,jdk.httpserver ^
      -Xlint:-incubating ^
      -cp "src;lib\imgui-java-binding.jar" ^
      -J-XX:+UseZGC -J-Xms4G -J-Xmx4G -J-XX:+AlwaysPreTouch ^
      @compile_release_list.txt

if %errorlevel% neq 0 (
    echo [ERROR] Production Compilation failed.
    exit /b %errorlevel%
)

:: 4. Inject Resources and Compute Shaders
echo [STAGE] Injecting static resources and Compute Shaders...
if not exist bin\sv\dark\ui mkdir bin\sv\dark\ui
copy /y src\sv\dark\ui\darkengine_logo.png bin\sv\dark\ui\darkengine_logo.png >nul

if not exist bin\sv\dark\admin mkdir bin\sv\dark\admin
copy /y src\sv\dark\admin\editor.html bin\sv\dark\admin\editor.html >nul
copy /y src\sv\dark\admin\index.html bin\sv\dark\admin\index.html >nul

:: Copy .comp shaders maintaining directory structure
echo [STAGE] Injecting GLSL Compute Shaders...
for /R src %%f in (*.comp) do (
    set "FILE_PATH=%%f"
    setlocal EnableDelayedExpansion
    set "REL_PATH=!FILE_PATH:%CD%\src\=!"
    set "DEST_DIR=%CD%\bin\!REL_PATH!"
    for %%a in ("!DEST_DIR!") do set "DEST_FOLDER=%%~dpa"
    if not exist "!DEST_FOLDER!" mkdir "!DEST_FOLDER!"
    copy /y "%%f" "!DEST_DIR!" >nul
    endlocal
)

:: 5. Inject ImGui Jars and Package as executable JAR
echo [STAGE] Injecting ImGui libraries...
cd bin
jar xf ..\lib\imgui-java-binding.jar
jar xf ..\lib\imgui-java-natives-windows.jar
if exist META-INF rd /s /q META-INF
cd ..
echo [STAGE] Creating DarkEngine-v1.0.jar...
jar --create --file DarkEngine-v1.0.jar --main-class sv.dark.state.DarkEngineMaster -C bin .

if %errorlevel% neq 0 (
    echo [ERROR] JAR creation failed.
    exit /b %errorlevel%
)

:: Isolate JAR for JPackage
if exist release_input rd /s /q release_input
mkdir release_input
move DarkEngine-v1.0.jar release_input\ >nul

:: 6. Generate Native App Image with JPackage
echo [STAGE] Generating Native Executable Image (JPackage)...
jpackage --name "Dark-Engine" ^
         --input release_input ^
         --main-jar DarkEngine-v1.0.jar ^
         --type app-image ^
         --dest Dark-Engine-V1.0 ^
         --win-console ^
         --add-modules jdk.incubator.vector,java.base,jdk.httpserver,jdk.management,java.desktop,jdk.unsupported ^
         --java-options "--enable-preview --enable-native-access=ALL-UNNAMED -XX:+UseZGC -Xms4G -Xmx4G -XX:+AlwaysPreTouch --add-modules jdk.incubator.vector"

if %errorlevel% neq 0 (
    echo [ERROR] jpackage failed.
    exit /b %errorlevel%
)

:: 7. Inject Native FFI Libraries (DLLs)
echo [STAGE] Injecting Native DLLs into the App Image...
if not exist Dark-Engine-V1.0\Dark-Engine\lib mkdir Dark-Engine-V1.0\Dark-Engine\lib
copy /y lib\glfw3.dll Dark-Engine-V1.0\Dark-Engine\lib\glfw3.dll >nul
copy /y lib\soft_oal.dll Dark-Engine-V1.0\Dark-Engine\lib\soft_oal.dll >nul

:: Cleanup temp files
if exist compile_release_list.txt del /q compile_release_list.txt

echo ---------------------------------------------------
echo [SUCCESS] Dark-Engine V1.0 Packaging Complete!
echo [READY] Run: Dark-Engine-V1.0\Dark-Engine\Dark-Engine.exe
echo ---------------------------------------------------
