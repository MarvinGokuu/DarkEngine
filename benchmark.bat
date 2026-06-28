@echo off
title DarkEngine JMH Micro-Profiler

:: Detect JDK
for /f "tokens=2 delims= " %%v in ('javac --version 2^>^&1') do set JAVAC_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVAC_VER%") do set JAVA_MAJOR=%%m

if not exist bin_jmh mkdir bin_jmh
if not exist src_jmh_generated mkdir src_jmh_generated

dir /s /B src\*.java > compile_list_jmh.txt

echo [BENCHMARK] Compiling AAA code and JMH annotations...
javac -d bin_jmh -s src_jmh_generated -encoding UTF-8 --enable-preview --source %JAVA_MAJOR% ^
    --add-modules jdk.incubator.vector ^
    -Xlint:-incubating ^
    -cp "src;lib\imgui-java-binding.jar;lib\jmh\*" ^
    -processor org.openjdk.jmh.generators.BenchmarkProcessor ^
    @compile_list_jmh.txt

if %errorlevel% neq 0 (
    echo [ERROR] JMH Compilation Failed.
    exit /b %errorlevel%
)

echo [BENCHMARK] Launching JMH Harness... (This will take a few minutes to measure nanoseconds correctly)
java -cp "bin_jmh;lib\imgui-java-binding.jar;lib\jmh\*" ^
    --enable-preview --add-modules jdk.incubator.vector ^
    --enable-native-access=ALL-UNNAMED ^
    -XX:+UseZGC -XX:+AlwaysPreTouch ^
    org.openjdk.jmh.Main %*

exit /b 0
