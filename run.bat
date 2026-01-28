@echo off
setlocal

echo.
echo VolcanEngine Runtime
echo ====================
echo.

set JVM_OPTS=--enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -XX:+UseZGC -Xms1G -Xmx1G

java %JVM_OPTS% -cp bin sv.volcan.state.VolcanEngineMaster

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Engine stopped with error code: %ERRORLEVEL%
)

endlocal
