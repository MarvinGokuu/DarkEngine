@echo off
set JAVA_CMD=java
set JAVA_MAJOR=25
javac -d bin -encoding UTF-8 --enable-preview --source %JAVA_MAJOR% --add-modules jdk.incubator.vector -Xlint:-incubating -cp "src;bin;lib\imgui-java-binding.jar" src\sv\dark\test\GracefulShutdownTest.java
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
%JAVA_CMD% -cp "bin;lib\imgui-java-binding.jar;lib\imgui-java-natives-windows.jar" --enable-preview --add-modules jdk.incubator.vector --enable-native-access=ALL-UNNAMED sv.dark.test.GracefulShutdownTest
