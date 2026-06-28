# script to download JMH dependencies for DarkEngine
$jmhVersion = "1.37"
$libDir = "$PSScriptRoot\..\lib\jmh"

If (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
}

Write-Host "[DarkEngine] Descargando JMH Core v$jmhVersion..."
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/openjdk/jmh/jmh-core/$jmhVersion/jmh-core-$jmhVersion.jar" -OutFile "$libDir\jmh-core-$jmhVersion.jar"

Write-Host "[DarkEngine] Descargando JMH Annotation Processor v$jmhVersion..."
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/openjdk/jmh/jmh-generator-annprocess/$jmhVersion/jmh-generator-annprocess-$jmhVersion.jar" -OutFile "$libDir\jmh-generator-annprocess-$jmhVersion.jar"

Write-Host "[DarkEngine] Descargando JOpt Simple (Dependencia de JMH)..."
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar" -OutFile "$libDir\jopt-simple-5.0.4.jar"

Write-Host "[DarkEngine] Descargando Commons Math3 (Dependencia de JMH)..."
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar" -OutFile "$libDir\commons-math3-3.6.1.jar"

Write-Host "[DarkEngine] JMH configurado exitosamente en /lib/jmh/."
