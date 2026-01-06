@echo off
title VOLCAN_ENGINE // SOVEREIGN_COMPILER v2.0
cls
color 0A

echo [SISTEMA] Iniciando Forja del Nucleo...

:: PASO 1: Limpieza de binarios (Solo la salida, nunca el src)
if exist bin rd /s /q bin
mkdir bin

::  PASO 2: Compilacion Integral (Java 25 + FFM)
:: No usamos taskkill aqui para no desconectar al agente
javac -d bin --enable-preview --source 25 -cp src ^
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

echo [OK] Compilacion exitosa. Nucleo estabilizado.
pause