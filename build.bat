@echo off
title Dark-Engine Compiler


:: Detect JDK
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m

:: Subsystem clear (Prevents JDK version mismatch and stale classes)
call clean.bat >nul 2>&1

:: Zombie process elimination has been disabled to prevent wmic hanging.

if not exist bin mkdir bin

<nul set /p="[BUILD] Compiling kernel and subsystems (JDK %JAVAC_VER%)... "

:: Auto-discover all Java files to prevent missing dependencies or OS command-line limits
dir /s /B src\*.java | findstr /v "\\test\\" | findstr /v "\\benchmark\\" > compile_list.txt

javac -d bin -encoding UTF-8 --enable-preview --source %JAVA_MAJOR% ^
    --add-modules jdk.incubator.vector ^
    -Xlint:-incubating ^
    -cp "src;lib\imgui-java-binding.jar" ^
    -J-XX:+UseZGC ^
    -J-Xms4G -J-Xmx4G ^
    -J-XX:+AlwaysPreTouch ^
    @compile_list.txt > compile.log 2>&1

if %errorlevel% neq 0 (
    echo [ERROR] BUILD FAILED. Critical compilation errors found.
    echo ----------------------------------------------
    type compile.log
    echo ----------------------------------------------
    exit /b %errorlevel%
)

if not exist bin\sv\dark\ui mkdir bin\sv\dark\ui
copy /y src\sv\dark\ui\darkengine_logo.png bin\sv\dark\ui\darkengine_logo.png >nul

if not exist bin\sv\dark\admin mkdir bin\sv\dark\admin
copy /y src\sv\dark\admin\editor.html bin\sv\dark\admin\editor.html >nul
copy /y src\sv\dark\admin\index.html bin\sv\dark\admin\index.html >nul

if exist compile.log del /q compile.log
if exist logs\clean.log del /q logs\clean.log

echo [OK] AAA+ Compiled.
exit /b 0
