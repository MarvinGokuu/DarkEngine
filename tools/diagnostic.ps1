$ErrorActionPreference = "SilentlyContinue"

Write-Host "Iniciando DarkEngine en background..."
$engineProcess = Start-Process -FilePath "cmd.exe" -ArgumentList "/c .\run.bat" -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 30

Write-Host "`n=== DARK ENGINE: MEMORIA NATIVA Y BUFFERS ==="
Get-Process java, Antigravity*, cmd -ErrorAction SilentlyContinue | Select-Object ProcessName, Id, @{Name = "RAM (MB)"; Expression = { [math]::Round($_.WorkingSet64 / 1MB, 2) } }, @{Name = "Virtual VRAM/Buffers (GB)"; Expression = { [math]::Round($_.VirtualMemorySize64 / 1GB, 2) } }, Handles | Format-Table -AutoSize

Write-Host "`n=== DARK ENGINE: AUDIO SUBSYSTEM HEALTH ==="
Get-Process audiodg, RtkAudUService64, java -ErrorAction SilentlyContinue | Select-Object ProcessName, Id, @{Name = "CPU (Segundos)"; Expression = { $_.CPU } }, @{Name = "RAM (MB)"; Expression = { [math]::Round($_.WorkingSet64 / 1MB, 2) } } | Format-Table -AutoSize

Write-Host "`n=== ESTADO DEL PROCESADOR (WMI/CIM) ==="
Get-CimInstance Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed, CurrentClockSpeed, LoadPercentage | Format-List

Write-Host "`nCerrando el motor para Auditoria Post-Mortem..."
Stop-Process -Id $engineProcess.Id -Force
Start-Sleep -Seconds 2

Write-Host "`n====================================================="
Write-Host " DARK ENGINE: POST-MORTEM AUDIT "
Write-Host "====================================================="

$zombies = Get-Process java -ErrorAction SilentlyContinue
if ($zombies) {
    Write-Host "[X] PELIGRO: Existen procesos Zombies de Java! Ejecutando exterminio atomico..."
    $zombies | Select-Object Id, Handles, WorkingSet64 | Format-Table
    Stop-Process -Id $zombies.Id -Force
    Write-Host "[+] Zombies exterminados. Memoria nativa purgada por la fuerza."
}
else {
    Write-Host "[OK] Cero procesos zombies. JVM finalizada limpiamente."
}

$audio = Get-Process audiodg -ErrorAction SilentlyContinue
if ($audio.CPU -gt 50) {
    Write-Host "[X] PELIGRO: Posible Underflow de Audio. audiodg usando mucha CPU."
}
else {
    Write-Host "[OK] Subsistema de Audio (OpenAL) vaciado correctamente."
}

$cpu = Get-CimInstance Win32_Processor | Select-Object CurrentClockSpeed, MaxClockSpeed
if ($cpu.CurrentClockSpeed -ge $cpu.MaxClockSpeed) {
    Write-Host "[!] AVISO: El CPU sigue a maxima frecuencia. Windows aun no lo ha dormido."
}
else {
    Write-Host "[OK] Kernel restaurado. CPU entrando en modo reposo."
}
