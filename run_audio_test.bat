@echo off
javac -d bin -encoding UTF-8 --enable-preview --source 25 --add-modules jdk.incubator.vector -Xlint:-incubating -cp "src;lib\imgui-java-binding.jar" src\sv\dark\core\systems\DarkAudioLinker.java src\sv\dark\audio\DarkOpenALBackend.java
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%

set JAVA_CMD=java --enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -cp "bin;lib\imgui-java-binding.jar;lib\imgui-java-natives-windows.jar"
%JAVA_CMD% sv.dark.test.SpatialAudioStressTest
powershell.exe -ExecutionPolicy Bypass -File .\audit_postmortem.ps1
