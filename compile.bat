@echo off
setlocal

echo [VOLCAN COMPILER] Iniciando Protocolo de Compilacion Sovereing...
echo [JDK] Version detectada:
java -version

if not exist "target\classes" (
    echo [IO] Creando directorio target/classes...
    mkdir "target\classes"
)

echo [JAVAC] Compilando fuentes con acceso nativo (Panama/Foreign API)...

javac --enable-native-access=ALL-UNNAMED ^
    -d target/classes ^
    src/sv/volcan/*.java ^
    src/sv/volcan/bus/*.java ^
    src/sv/volcan/core/*.java ^
    src/sv/volcan/core/memory/*.java ^
    src/sv/volcan/core/systems/*.java ^
    src/sv/volcan/kernel/*.java ^
    src/sv/volcan/net/*.java ^
    src/sv/volcan/state/*.java

if %ERRORLEVEL% EQU 0 (
    echo [EXITO] Volcan Engine compilado correctamente.
    echo [INFO] Artefactos en target/classes
) else (
    echo [FALLO] Error durante la compilacion. Revisa los errores arriba.
    exit /b %ERRORLEVEL%
)

endlocal
