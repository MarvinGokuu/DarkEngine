@echo off
REM Script de Limpieza y Commits Incrementales para Windows
REM Ejecutar desde: c:\Users\theca\Documents\GitHub\VolcanEngine

title VOLCAN ENGINE - COMMITS INCREMENTALES
cls
color 0B

echo ==========================================
echo VOLCAN ENGINE - COMMITS INCREMENTALES
echo ==========================================
echo.

REM PASO 0: Limpiar staging area
echo [0/11] Limpiando staging area...
git reset
echo [OK] Staging area limpiado
echo.
pause

REM COMMIT 1: v0.1 - Fundacion
echo [1/11] Commit v0.1 - Fundacion del Proyecto...
git add .gitignore README.md SovereignProtocol.bat compile.bat ignite.bat Sovereign_Protocol_Manifest.txt
git commit -m "[CHORE] v0.1 - Fundacion del Proyecto Volcan Engine"
git tag -a v0.1-foundation -m "Volcan Engine v0.1 - Fundacion"
echo [OK] v0.1 completado
echo.
pause

REM COMMIT 2: v0.2 - Kernel
echo [2/11] Commit v0.2 - Kernel Soberano...
git add src/sv/volcan/kernel/
git commit -m "[FEAT] v0.2 - Implementar Kernel Soberano con Fixed Timestep"
git tag -a v0.2-kernel -m "Volcan Engine v0.2 - Kernel"
echo [OK] v0.2 completado
echo.
pause

REM COMMIT 3: v0.3 - Memoria
echo [3/11] Commit v0.3 - Sistema de Memoria Off-Heap...
git add src/sv/volcan/state/
git commit -m "[FEAT] v0.3 - Sistema de Memoria Off-Heap con Project Panama"
git tag -a v0.3-memory -m "Volcan Engine v0.3 - Memoria Off-Heap"
echo [OK] v0.3 completado
echo.
pause

REM COMMIT 4: v0.4 - Bus Basico
echo [4/11] Commit v0.4 - Bus Atomico Lock-Free...
git add src/sv/volcan/bus/IEventBus.java src/sv/volcan/bus/VolcanAtomicBus.java src/sv/volcan/bus/VolcanRingBus.java
git commit -m "[FEAT] v0.4 - Bus Atomico Lock-Free con Cache Line Padding"
git tag -a v0.4-bus -m "Volcan Engine v0.4 - Bus Atomico"
echo [OK] v0.4 completado
echo.
pause

REM COMMIT 5: v0.5 - Multi-Lane
echo [5/11] Commit v0.5 - Sistema Multi-Lane...
git add src/sv/volcan/bus/VolcanEventType.java src/sv/volcan/bus/BackpressureStrategy.java src/sv/volcan/bus/VolcanEventLane.java src/sv/volcan/bus/VolcanEventDispatcher.java
git commit -m "[FEAT] v0.5 - Sistema Multi-Lane con Backpressure"
git tag -a v0.5-multilane -m "Volcan Engine v0.5 - Multi-Lane"
echo [OK] v0.5 completado
echo.
pause

REM COMMIT 6: v0.6 - Signals
echo [6/11] Commit v0.6 - Signal Dispatcher AAA+...
git add src/sv/volcan/bus/VolcanSignalPacker.java src/sv/volcan/bus/VolcanSignalCommands.java src/sv/volcan/bus/SignalProcessor.java src/sv/volcan/bus/VolcanSignalDispatcher.java
git commit -m "[FEAT] v0.6 - Signal Dispatcher AAA+ sin Boxing"
git tag -a v0.6-signals -m "Volcan Engine v0.6 - Signal Dispatcher"
echo [OK] v0.6 completado
echo.
pause

REM COMMIT 7: v0.7 - Systems
echo [7/11] Commit v0.7 - Sistemas de Juego...
git add src/sv/volcan/core/systems/ src/sv/volcan/core/EntityLayout.java
git commit -m "[FEAT] v0.7 - Sistemas de Juego Basicos"
git tag -a v0.7-systems -m "Volcan Engine v0.7 - Game Systems"
echo [OK] v0.7 completado
echo.
pause

REM COMMIT 8: v0.8 - Tests
echo [8/11] Commit v0.8 - Tests de Hardware...
git add src/sv/volcan/bus/Test_BusHardware.java src/sv/volcan/bus/Test_BusCoordination.java
git commit -m "[TEST] v0.8 - Tests de Validacion de Hardware"
git tag -a v0.8-tests -m "Volcan Engine v0.8 - Hardware Tests"
echo [OK] v0.8 completado
echo.
pause

REM COMMIT 9: v0.9 - Docs
echo [9/11] Commit v0.9 - Documentacion AAA+...
git add src/sv/volcan/test/documentacion/AAA_CERTIFICATION.md src/sv/volcan/test/documentacion/AAA_CODING_STANDARDS.md src/sv/volcan/test/documentacion/ESTANDAR_DOCUMENTACION.md src/sv/volcan/test/documentacion/GUIA_COMMITS.md src/sv/volcan/test/documentacion/ARQUITECTURA_VOLCAN_ENGINE.md src/sv/volcan/test/documentacion/DOCUMENTACION_BUS.md src/sv/volcan/test/documentacion/SIGNAL_DISPATCHER_GUIDE.md src/sv/volcan/test/documentacion/TECHNICAL_GLOSSARY.md
git commit -m "[DOCS] v0.9 - Documentacion Completa AAA+"
git tag -a v0.9-docs -m "Volcan Engine v0.9 - Documentacion AAA+"
echo [OK] v0.9 completado
echo.
pause

REM COMMIT 10: v0.10 - Workflow
echo [10/11] Commit v0.10 - Herramientas de Workflow...
git add src/sv/volcan/test/documentacion/FLUJO_TRABAJO.md src/sv/volcan/test/documentacion/GUIA_UPDATE_SYNC.md src/sv/volcan/test/documentacion/LISTA_PENDIENTES.md src/sv/volcan/test/documentacion/ACTUALIZACIONES_PENDIENTES.md update.bat GIT_PRIMER_COMMIT.md PRIMER_COMMIT.txt ESTRATEGIA_COMMITS.md
git commit -m "[CHORE] v0.10 - Herramientas de Workflow y Sincronizacion"
git tag -a v0.10-workflow -m "Volcan Engine v0.10 - Workflow Tools"
echo [OK] v0.10 completado
echo.
pause

REM COMMIT 11: v1.0 - Todo lo demas
echo [11/11] Commit v1.0 - Certificacion AAA+...
git add .
git commit -m "[FEAT] v1.0 - Volcan Engine AAA+ Certified"
git tag -a v1.0-aaa-certified -m "Volcan Engine v1.0 - AAA+ Certified"
echo [OK] v1.0 completado
echo.

REM Mostrar resumen
echo ==========================================
echo RESUMEN DE COMMITS
echo ==========================================
git log --oneline --graph --all
echo.
echo ==========================================
echo TAGS CREADOS
echo ==========================================
git tag
echo.
echo [SUCCESS] Todos los commits completados exitosamente!
pause
