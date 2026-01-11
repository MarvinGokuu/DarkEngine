@echo off
REM ═════════════════════════════════════════════════════════════════════════
REM PROTOCOLO DE HIGIENE AAA+ (ZERO-NOISE)
REM Autoridad: Marvin-Dev
REM Propósito: Restaurar la Soberanía del Espacio de Trabajo
REM ═════════════════════════════════════════════════════════════════════════

echo [HYGIENE] Iniciando Protocolo de Limpieza...

REM 1. Eliminar Artefactos Muertos (Archivos Fantasma)
if exist "cd" (
    echo [DELETE] Eliminando archivo fantasma 'cd'...
    del "cd"
)
if exist "move" (
    echo [DELETE] Eliminando archivo fantasma 'move'...
    del "move"
)
if exist "sync_report_*.txt" (
    echo [DELETE] Eliminando logs antiguos...
    del "sync_report_*.txt"
)
if exist "Sovereign_Protocol_Manifest.txt" (
    echo [ARCHIVE] Moviendo manifiesto a docs...
    move "Sovereign_Protocol_Manifest.txt" "docs\manuals\"
)

REM 2. Estructura de Herramientas
if not exist "tools\visual-observer" mkdir "tools\visual-observer"
if not exist "tools\metrics" mkdir "tools\metrics"

REM 3. Migración de Visual Observer
if exist "VisualObserver.html" (
    echo [MOVE] Migrando VisualObserver.html...
    move "VisualObserver*.html" "tools\visual-observer\"
)

REM 4. Migración de Métricas
if exist "VolcanMetricsClient.js" (
    echo [MOVE] Migrando Cliente JS...
    move "VolcanMetricsClient.js" "tools\metrics\"
)

echo.
echo [HYGIENE] Protocolo Completado. 
echo [HYGIENE] El Espacio de Trabajo es ahora Soberano.
pause
