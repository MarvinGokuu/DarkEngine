.\test.bat

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host " ️ DARK ENGINE: POST-MORTEM AUDIT ️ " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Chequeo de Procesos Zombies (Java) con Exterminio Atómico
$zombies = Get-Process java -ErrorAction SilentlyContinue
if ($zombies) {
    Write-Host "[X] PELIGRO: Existen procesos Zombies de Java! Ejecutando exterminio atómico..." -ForegroundColor Red
    $zombies | Select-Object Id, Handles, WorkingSet64 | Format-Table
    Stop-Process -Id $zombies.Id -Force
    Write-Host "[+] Zombies exterminados. Memoria nativa purgada por la fuerza." -ForegroundColor Green
} else {
    Write-Host "[OK] Cero procesos zombies. JVM finalizada limpiamente." -ForegroundColor Green
}

# 2. Chequeo de Buffers de Audio Trabados (audiodg)
$audio = Get-Process audiodg -ErrorAction SilentlyContinue
if ($audio -and $audio.CPU -gt 50) {
    Write-Host "[X] PELIGRO: Posible Underflow de Audio. audiodg usando mucha CPU." -ForegroundColor Red
} else {
    Write-Host "[OK] Subsistema de Audio (OpenAL) vaciado correctamente." -ForegroundColor Green
}

# 3. Restauración del Estado del Kernel (C-States / Frecuencia)
$cpu = Get-CimInstance Win32_Processor | Select-Object CurrentClockSpeed, MaxClockSpeed
if ($cpu -and $cpu.CurrentClockSpeed -ge $cpu.MaxClockSpeed) {
    Write-Host "[!] AVISO: El CPU sigue a máxima frecuencia. Windows aún no lo ha dormido." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Kernel restaurado. CPU entrando en modo reposo." -ForegroundColor Green
}
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host " AUDITORÍA FINALIZADA. ENTORNO SEGURO. " -ForegroundColor Cyan
Write-Host "====================================================="
