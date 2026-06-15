@echo off
title Dark-Engine Compiler


:: Detect JDK
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m

:: Subsystem clear (Prevents JDK version mismatch and stale classes)
call clean.bat >nul 2>&1

:: Zombie process elimination (Strict match for DarkEngineMaster inside JVM)
for /f "tokens=2 delims=," %%p in ('wmic process where "Name='java.exe' or Name='javaw.exe'" get ProcessId^,CommandLine /format:csv 2^>nul ^| findstr /i "DarkEngineMaster"') do (
    echo [SYSTEM] Eliminating lingering zombie process [PID: %%p]
    taskkill /F /PID %%p >nul 2>&1
)

if not exist bin mkdir bin

<nul set /p="[BUILD] Compiling kernel and subsystems (JDK %JAVAC_VER%)... "
javac -d bin -encoding UTF-8 --enable-preview --source %JAVA_MAJOR% ^
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
    src\sv\dark\scene\*.java ^
    src\sv\dark\net\*.java ^
    src\sv\dark\test\*.java ^
    src\sv\dark\ui\*.java ^
    src\sv\dark\admin\*.java > compile.log 2>&1

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
