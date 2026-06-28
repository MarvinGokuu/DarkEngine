Write-Host "=== DARK ENGINE: MEMORIA NATIVA Y BUFFERS ==="
Get-Process java -ErrorAction SilentlyContinue | Select-Object ProcessName, Id, @{Name="RAM (MB)"; Expression={[math]::Round($_.WorkingSet64 / 1MB, 2)}}, @{Name="Virtual VRAM/Buffers (GB)"; Expression={[math]::Round($_.VirtualMemorySize64 / 1GB, 2)}}, Handles | Format-Table -AutoSize

Write-Host "=== DARK ENGINE: AUDIO SUBSYSTEM HEALTH ==="
Get-Process audiodg -ErrorAction SilentlyContinue | Select-Object ProcessName, Id, @{Name="CPU (Segundos)"; Expression={$_.CPU}}, @{Name="RAM (MB)"; Expression={[math]::Round($_.WorkingSet64 / 1MB, 2)}} | Format-Table -AutoSize

Write-Host "=== ESTADO DEL PROCESADOR (WMI/CIM) ==="
Get-CimInstance Win32_Processor | Select-Object Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed, CurrentClockSpeed, LoadPercentage | Format-List
