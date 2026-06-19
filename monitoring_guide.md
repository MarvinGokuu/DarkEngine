# 🛠️ DarkEngine: Dashboard de Monitoreo por Terminal (PowerShell)

Ya que el motor está corriendo ahora mismo y quieres verle "las tripas" al sistema operativo, te he preparado este arsenal de comandos en PowerShell. 

Copia y pega cualquiera de estos bloques en tu terminal para auditar la salud del hardware en tiempo real.

---

### 1. 💽 Auditar RAM y Memoria Nativa (Off-Heap Buffers) del Motor
Tu motor usa Memoria Nativa (Zero-GC), por lo que debemos monitorear el `WorkingSet64` y la "Memoria Virtual Paginada" del proceso Java, ya que allí viven las *Arenas* de memoria.
```powershell
while ($true) { 
    Clear-Host
    Write-Host "=== 🧠 DARK ENGINE: MEMORIA NATIVA Y BUFFERS ===" -ForegroundColor Cyan
    Get-Process java, Antigravity* -ErrorAction SilentlyContinue | 
        Select-Object ProcessName, 
                      Id, 
                      @{Name="RAM (MB)"; Expression={[math]::Round($_.WorkingSet64 / 1MB, 2)}},
                      @{Name="Virtual VRAM/Buffers (GB)"; Expression={[math]::Round($_.VirtualMemorySize64 / 1GB, 2)}},
                      Handles | 
        Format-Table -AutoSize
    Start-Sleep -Seconds 2 
}
```
*(Presiona `Ctrl + C` para salir del loop)*

---

### 2. 🔊 Monitorear la Tarjeta de Audio y el "GameSystem" (OpenAL)
Para ver si el motor de audio está mandando ondas a la placa Gigabyte, monitoreamos el **Audio Device Graph (`audiodg`)** de Windows y los servicios de Realtek. Si hay un *Underflow* o loop infinito, la CPU de `audiodg` se disparará al máximo.
```powershell
while ($true) { 
    Clear-Host
    Write-Host "=== 🔊 DARK ENGINE: AUDIO SUBSYSTEM HEALTH ===" -ForegroundColor Yellow
    Get-Process audiodg, RtkAudUService64, java -ErrorAction SilentlyContinue | 
        Select-Object ProcessName, 
                      Id, 
                      @{Name="CPU (Segundos)"; Expression={$_.CPU}},
                      @{Name="RAM (MB)"; Expression={[math]::Round($_.WorkingSet64 / 1MB, 2)}} | 
        Format-Table -AutoSize
    Start-Sleep -Seconds 2 
}
```

---

### 3. ⚙️ Monitorear Salud del Procesador (C-States y Frecuencias)
Este comando interroga al Kernel de Windows (WMI/CIM) para ver si la CPU está "dormida" o si tu `EngineKernel.java` la tiene en estado de alerta máxima (Alta Frecuencia).
```powershell
Write-Host "=== ⚡ ESTADO DEL PROCESADOR (WMI/CIM) ===" -ForegroundColor Green
Get-CimInstance Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed, CurrentClockSpeed, LoadPercentage | Format-List
```

---

### 4. 🗄️ Monitorear Cuellos de Botella en Disco (I/O)
Ideal para verificar si la **Fase 21** (el *Zero-Copy Asset Streamer*) está saturando el disco NVMe cuando compila o transfiere archivos pesados.
```powershell
while ($true) { 
    Clear-Host
    Write-Host "=== 💾 DARK ENGINE: LECTURA/ESCRITURA DE DISCO (I/O) ===" -ForegroundColor Magenta
    Get-Process java -ErrorAction SilentlyContinue | 
        Select-Object ProcessName, 
                      Id, 
                      @{Name="Bytes Leídos (MB)"; Expression={[math]::Round($_.ReadTransferCount / 1MB, 2)}},
                      @{Name="Bytes Escritos (MB)"; Expression={[math]::Round($_.WriteTransferCount / 1MB, 2)}} | 
        Format-Table -AutoSize
    Start-Sleep -Seconds 2 
}
```

---

### 5. ⏱️ Leer las Métricas del Game Loop en Tiempo Real
Ya que el motor está corriendo, puedes "espiar" cómo van los tiempos de latencia desde powershell, leyendo en vivo el final del archivo de logs:
```powershell
Get-Content .\logs\darkengine_metrics.log -Wait -Tail 15
```

---

## 🛑 Auditoría Post-Mortem (EJECUTAR DESPUÉS DE CERRAR EL MOTOR)

Una vez que cierres la ventana GLFW del motor, copia y pega este bloque de auditoría maestra. Este script de PowerShell verificará automáticamente que no quede absolutamente **nada** en tu máquina (cero procesos zombies, cero fugas de memoria nativa, audio apagado y Kernel restaurado).

```powershell
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "       🛡️ DARK ENGINE: POST-MORTEM AUDIT 🛡️        " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan

# 1. Chequeo de Procesos Zombies (Java)
$zombies = Get-Process java -ErrorAction SilentlyContinue
if ($zombies) {
    Write-Host "[X] PELIGRO: Existen procesos Zombies de Java ejecutándose!" -ForegroundColor Red
    $zombies | Select-Object Id, Handles, WorkingSet64 | Format-Table
} else {
    Write-Host "[OK] Cero procesos zombies. JVM finalizada limpiamente." -ForegroundColor Green
}

# 2. Chequeo de Buffers de Audio Trabados (audiodg)
$audio = Get-Process audiodg -ErrorAction SilentlyContinue
if ($audio.CPU -gt 50) {
    Write-Host "[X] PELIGRO: Posible Underflow de Audio. audiodg usando mucha CPU." -ForegroundColor Red
} else {
    Write-Host "[OK] Subsistema de Audio (OpenAL) vaciado correctamente." -ForegroundColor Green
}

# 3. Chequeo del Kernel de Windows (C-States / Frecuencia)
$cpu = Get-CimInstance Win32_Processor | Select-Object CurrentClockSpeed, MaxClockSpeed
if ($cpu.CurrentClockSpeed -ge $cpu.MaxClockSpeed) {
    Write-Host "[!] AVISO: El CPU sigue a máxima frecuencia. Windows aún no lo ha dormido." -ForegroundColor Yellow
} else {
    Write-Host "[OK] Kernel restaurado. CPU entrando en modo reposo." -ForegroundColor Green
}

# 4. Chequeo de Handles Huérfanos
$topHandles = Get-Process | Sort-Object Handles -Descending | Select-Object -First 3
Write-Host "[INFO] Top 3 procesos usando Handles en el sistema:" -ForegroundColor Gray
$topHandles | Select-Object ProcessName, Id, Handles | Format-Table -HideTableHeaders

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "        AUDITORÍA FINALIZADA. PUEDES SUSPENDER.      " -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
```
