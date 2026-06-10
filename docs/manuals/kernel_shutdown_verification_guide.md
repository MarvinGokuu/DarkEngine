# Guía de Verificación: Auditoría de Limpieza del Núcleo (Shutdown Cleanliness)

Esta guía establece el protocolo ordenado para verificar que el núcleo de **DarkEngine** se detiene de forma limpia, liberando todos los recursos del sistema operativo (hilos, sockets y memoria) sin dejar rastros residuales ("procesos zombies" o "zombies threads").

---

## 1. Verificación del Puerto de Red (Socket Cleanliness)
El motor utiliza el puerto `8080` para exponer las métricas en tiempo real mediante `DarkMetricsServer`. Un apagado sucio dejaría el socket abierto, impidiendo reiniciar el motor de inmediato.

### Protocolo de Prueba:
1. **Ejecutar el motor** (`.\run.bat`).
2. En CMD o PowerShell, verificar que el puerto esté activo y capturar el PID (Process ID):
   ```cmd
   netstat -ano | findstr 8080
   ```
   *Resultado esperado:* Debe devolver líneas con estado `LISTENING` acompañadas del PID del proceso (ej. `8400`).
3. **Cerrar el motor** (usando la "X" de la ventana).
4. Ejecutar el comando nuevamente:
   ```cmd
   netstat -ano | findstr 8080
   ```
   *Resultado esperado:* **Ninguna salida**. Esto certifica que el puerto `8080` ha sido devuelto al sistema operativo inmediatamente.

---

## 2. Auditoría de Procesos en el Sistema Operativo (OS Cleanliness)
Java puede mantener la máquina virtual (JVM) activa en segundo plano si existen hilos que no son de tipo daemon (`non-daemon threads`) activos después de cerrar la interfaz gráfica.

### Protocolo de Prueba:
1. Con el motor **cerrado**, ejecuta el siguiente comando en PowerShell para auditar los argumentos de inicio de todos los procesos Java activos:
   ```powershell
   Get-CimInstance Win32_Process -Filter "name='java.exe' or name='javaw.exe'" | Select-Object ProcessId, CommandLine | Format-Table -Wrap
   ```
2. **Análisis de Resultados:**
   * **Procesos del IDE (Permitidos):** Verás procesos con argumentos largos apuntando a `redhat.java` o `eclipse.jdt.ls`. Esto es normal (es la extensión de Java de tu IDE).
   * **Procesos del Motor (Fuga si aparecen):** Si encuentras algún PID que contenga `--enable-preview` y `sv.dark.state.DarkEngineMaster` en su `CommandLine`, significa que hay una fuga de hilos que mantiene vivo al motor.
   * *Resultado esperado:* Solo deben figurar los procesos del IDE y de las herramientas que estás usando.

---

## 3. Auditoría de Firma de Logs (Application Cleanliness)
El archivo `darkengine.log` registra la secuencia exacta de desmontaje de los subsistemas en el orden inverso al que fueron inicializados.

### Protocolo de Prueba:
Inspecciona el final del archivo `darkengine.log` tras cerrar el motor. Debes verificar la presencia de las siguientes líneas de firma:

```log
>>> INICIANDO SECUENCIA DE APAGADO SEGURO...
[ADMIN] Stopping DarkMetricsServer...
[METRICS GATEWAY] Stopped
...
[STEP 6/6] Sector Vault cerrado ✓
[ADMIN] Bus cerrado - AdminConsumer terminando
═══════════════════════════════════════════════════════════════
[KERNEL] GRACEFUL SHUTDOWN COMPLETED
```

* **Línea Clave 1 (`[ADMIN] Bus cerrado - AdminConsumer terminando`):** Confirma que el hilo de administración (`AdminConsumer`) salió de su bucle de lectura sin bloquearse.
* **Línea Clave 2 (`[KERNEL] GRACEFUL SHUTDOWN COMPLETED`):** Es la última instrucción ejecutada por el hook de apagado (`Shutdown Hook`). Confirma el desmantelamiento total de las estructuras del núcleo.

---

## 4. Auditoría de CPU en Reposo (Resource Idle State)
Un bucle infinito o de consulta constante (*busy-spin*) en los hilos del despachador o administración sobrecalentará el procesador y degradará el rendimiento.

### Protocolo de Prueba:
1. Abre el **Administrador de Tareas** y ve a la pestaña **Detalles**.
2. Abre el motor (`.\run.bat`).
3. Busca el proceso `javaw.exe` / `java.exe` de tu motor y observa su columna de **CPU** en reposo (sin interactuar con él).
   * *Resultado esperado:* El consumo debe estar entre **0% y 1%**.
   * *Fuga detectada (si ocurre):* Si la CPU se mantiene de forma sostenida arriba del **12% o 25%** (equivalente al 100% de uso de un hilo/núcleo lógico), indica un error de busy-spin en los canales de eventos o de administración.
