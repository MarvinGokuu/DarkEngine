@echo off
setlocal

:: Dark-Engine dev runner — sin consola visible (javaw = GUI subsystem)
:: Log disponible en: darkengine.log

set JVM_OPTS=--enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -XX:+UseZGC -Xms1G -Xmx1G

start "Dark-Engine" javaw %JVM_OPTS% -cp bin sv.dark.state.DarkEngineMaster

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Engine failed to launch. Check darkengine.log for details.
    pause
)

endlocal
