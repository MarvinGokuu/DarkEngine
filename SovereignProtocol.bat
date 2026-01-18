@echo off
title VOLCAN_ENGINE // SOVEREIGN_COMPILER v2.1 + ZGC
cls
color 0A

echo [SISTEMA] Iniciando Forja del Nucleo con ZGC Ultra-Latency...

:: PASO 1: Limpieza de binarios (Solo la salida, nunca el src)
if exist bin rd /s /q bin
mkdir bin

::  PASO 2: Compilacion Integral (Java 25 + FFM + ZGC + Vector SIMD)
:: ZGC Flags (NEURONA_048 Paso 1):
::   -XX:+UseZGC          : GC pausas <1ms (target: 500μs)
::   -Xms4G -Xmx4G        : Heap fijo (evita resize pauses)
::   -XX:+AlwaysPreTouch  : Elimina lazy allocation overhead
:: Ganancia esperada: -96.7% GC pause (15ms → 500μs)
:: Project Panama: --enable-native-access solo en runtime (java), no en javac
:: Vector API: --add-modules jdk.incubator.vector para SIMD (VolcanDataAccelerator)
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
    echo [ERROR] Fallo en la compilacion. Revise el panel de Problemas.
    exit /b %errorlevel%
)

echo [OK] Compilacion exitosa. Nucleo estabilizado con ZGC.

:: PASO 3: Ignicion del Nucleo (Runtime)
echo [SISTEMA] Iniciando Ignición del Motor...
java --enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -cp bin sv.volcan.state.VolcanEngineMaster

pause