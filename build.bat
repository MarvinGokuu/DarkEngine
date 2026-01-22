@echo off
title VolcanEngine Compiler v2.3-mvp
cls

echo.
echo VolcanEngine Build System v2.3-mvp
echo ===================================
echo.

:: Clean previous build
if exist bin rd /s /q bin
mkdir bin

:: Compile with Java 25 + ZGC + Vector API
echo Compiling...
javac -d bin --enable-preview --source 25 --add-modules jdk.incubator.vector -cp src ^
-J-XX:+UseZGC ^
-J-Xms4G -J-Xmx4G ^
-J-XX:+AlwaysPreTouch ^
src\sv\volcan\state\VolcanEngineMaster.java ^
src\sv\volcan\kernel\*.java ^
src\sv\volcan\core\*.java ^
src\sv\volcan\core\memory\*.java ^
src\sv\volcan\core\systems\*.java ^
src\sv\volcan\state\*.java ^
src\sv\volcan\bus\*.java ^
src\sv\volcan\net\*.java ^
src\sv\volcan\test\*.java

if %errorlevel% neq 0 (
    echo.
    echo Build failed. Check errors above.
    exit /b %errorlevel%
)

echo Build successful.
echo.

:: Run engine
echo Starting VolcanEngine...
java --enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -cp bin sv.volcan.state.VolcanEngineMaster

pause
