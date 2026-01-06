@echo off
title VOLCAN ENGINE - UPDATE SYNC v1.0
cls
color 0B

echo ═══════════════════════════════════════════════════════════
echo   VOLCAN ENGINE - DOCUMENTATION SYNC TOOL
echo ═══════════════════════════════════════════════════════════
echo.

:: ============================================================
:: PASO 1: Verificar estructura de directorios
:: ============================================================
echo [1/5] Verificando estructura de directorios...

if not exist "src\sv\volcan\test\documentacion" (
    echo [ERROR] Directorio de documentacion no encontrado
    exit /b 1
)

if not exist "src\sv\volcan\bus" (
    echo [ERROR] Directorio de bus no encontrado
    exit /b 1
)

if not exist "src\sv\volcan\kernel" (
    echo [ERROR] Directorio de kernel no encontrado
    exit /b 1
)

echo [OK] Estructura de directorios validada
echo.

:: ============================================================
:: PASO 2: Listar archivos de documentacion
:: ============================================================
echo [2/5] Listando archivos de documentacion...

set DOC_DIR=src\sv\volcan\test\documentacion
set DOC_COUNT=0

for %%f in (%DOC_DIR%\*.md) do (
    set /a DOC_COUNT+=1
)

echo [OK] Encontrados %DOC_COUNT% archivos de documentacion
echo.

:: ============================================================
:: PASO 3: Verificar archivos criticos
:: ============================================================
echo [3/5] Verificando archivos criticos...

set CRITICAL_DOCS=AAA_CERTIFICATION.md AAA_CODING_STANDARDS.md ESTANDAR_DOCUMENTACION.md ACTUALIZACIONES_PENDIENTES.md GUIA_COMMITS.md LISTA_PENDIENTES.md

for %%d in (%CRITICAL_DOCS%) do (
    if exist "%DOC_DIR%\%%d" (
        echo [OK] %%d
    ) else (
        echo [WARN] %%d - NO ENCONTRADO
    )
)
echo.

:: ============================================================
:: PASO 4: Verificar archivos de codigo criticos
:: ============================================================
echo [4/5] Verificando archivos de codigo criticos...

set CRITICAL_CODE=src\sv\volcan\bus\VolcanAtomicBus.java src\sv\volcan\bus\VolcanRingBus.java src\sv\volcan\bus\Test_BusHardware.java src\sv\volcan\kernel\SovereignKernel.java

for %%c in (%CRITICAL_CODE%) do (
    if exist "%%c" (
        echo [OK] %%c
    ) else (
        echo [WARN] %%c - NO ENCONTRADO
    )
)
echo.

:: ============================================================
:: PASO 5: Generar reporte de sincronizacion
:: ============================================================
echo [5/5] Generando reporte de sincronizacion...

set REPORT_FILE=sync_report_%date:~-4,4%%date:~-10,2%%date:~-7,2%.txt

echo VOLCAN ENGINE - SYNC REPORT > %REPORT_FILE%
echo Fecha: %date% %time% >> %REPORT_FILE%
echo ═══════════════════════════════════════════════════════════ >> %REPORT_FILE%
echo. >> %REPORT_FILE%

echo DOCUMENTACION: >> %REPORT_FILE%
dir /b %DOC_DIR%\*.md >> %REPORT_FILE%
echo. >> %REPORT_FILE%

echo CODIGO CRITICO: >> %REPORT_FILE%
for %%c in (%CRITICAL_CODE%) do (
    if exist "%%c" (
        echo [OK] %%c >> %REPORT_FILE%
    ) else (
        echo [FALTA] %%c >> %REPORT_FILE%
    )
)

echo [OK] Reporte generado: %REPORT_FILE%
echo.

:: ============================================================
:: RESUMEN
:: ============================================================
echo ═══════════════════════════════════════════════════════════
echo   RESUMEN DE SINCRONIZACION
echo ═══════════════════════════════════════════════════════════
echo.
echo Documentos encontrados: %DOC_COUNT%
echo Reporte generado: %REPORT_FILE%
echo.
echo [INFO] Revisa el reporte para detalles completos
echo ═══════════════════════════════════════════════════════════
echo.

pause
