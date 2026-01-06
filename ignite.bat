@echo off
setlocal

echo [VOLCAN IGNITION] Iniciando Secuencia de Arranque...

:: CONFIGURACION DE MEMORIA Y VECTOR API
set JVM_OPTS=--enable-native-access=ALL-UNNAMED -XX:+UseVectorApiIntrinsics -Xms1G -Xmx1G

echo [JVM] Opciones: %JVM_OPTS%
echo [CLASE PRINCIPAL] sv.volcan.VolcanEngineMaster

java %JVM_OPTS% ^
    -cp target/classes ^
    sv.volcan.VolcanEngineMaster

if %ERRORLEVEL% NEQ 0 (
    echo [CRITICO] El nucleo se detuvo inesperadamente. Codigo: %ERRORLEVEL%
)

endlocal
