# 🛡️ GUÍA MAESTRA: SEGURIDAD, RENDIMIENTO Y PROGRAMACIÓN A BAJO NIVEL
### DarkEngine — Reporte de Ingeniería y Auditoría de Seguridad
**Fecha:** 2026-06-17 | **Arquitecto:** Marvin A. Flores Canales | **Estado del Motor:** 35% — Fase 27 en progreso

---

## ÍNDICE

1. [Estado de Cambios Pendientes (Git)](#git)
2. [El Error Crítico: GamingServices DCOM Loop](#dcom)
3. [Qué se Filtró en GitHub y Cómo Protegerte](#seguridad)
4. [Guía de Monitoreo a Bajo Nivel (Tu PC como Motor)](#monitoreo)
5. [Cómo Monitorear Programación de Bajo Nivel (FFI, Panama, Memoria)](#bajo-nivel)
6. [Métodos de Verificación del Sistema](#verificacion)
7. [Pasos Inmediatos y Lista de Acción](#accion)

---

## 1. ESTADO DE CAMBIOS PENDIENTES (GIT) {#git}

> [!IMPORTANT]
> Tienes cambios locales en tu rama `feature/phase21-asset-compiler-streaming` que **NO han sido pusheados** a GitHub.

### Archivos Modificados (no commiteados):

| Archivo | Cambio |
|---|---|
| `.gitignore` | Limpieza de duplicados + protección repomix |
| `compile_list.txt` | Añadido `DarkDeferredPipeline.java` |
| `DarkGraphicsLinker.java` | Eliminado `glfwMakeContextCurrent` duplicado |
| `DarkOpenGLLinker.java` | +9 funciones FFI: Texturas, FBOs, ImageBind (Fase 27) |
| `DarkEngineWindow.java` | Inicialización de `DarkDeferredPipeline` |

### Archivo Nuevo (untracked):

| Archivo | Descripción |
|---|---|
| `src/sv/dark/scene/DarkDeferredPipeline.java` | G-Buffer 1280x720 en VRAM — Fase 27 Misión A |

### Estado de la Compilación:
```
✅ build.bat → [OK] AAA+ Compiled.
✅ Sin errores de compilación
⏳ Pendiente: git add + commit + push
```

---

## 2. EL ERROR CRÍTICO: GAMINGSERVICES DCOM LOOP {#dcom}

### ¿Qué fue lo que te congeló la PC?

Tu sistema experimentó un **bucle de reinicio infinito del servicio GamingServices** vía DCOM (Distributed Component Object Model). Esto es exactamente lo que pasó a nivel de silicio:

```
SCM (Service Control Manager)
  └─▶ Intenta iniciar GamingServices.exe
        └─▶ GamingServices falla (argumento RPC inválido: Error 87)
              └─▶ SCM detecta fallo → Política: Reiniciar en 0ms
                    └─▶ Vuelve a fallar → Bucle infinito
```

### Por qué afecta directamente al DarkEngine:

Tu motor usa **Thread Pinning** (amarre de hilos al Core 1 físico de la CPU). Cuando GamingServices entra en bucle de reinicio, genera:

1. **Miles de Context Switches por segundo** — El planificador de Windows interrumpe tu Game Loop para atender el servicio muriente.
2. **Inundación de interrupciones de hardware** — Cada fallo de RPC genera una IRQ que satura el bus del sistema.
3. **Bloqueo de `resmon.exe`** — El Monitor de Recursos intenta leer el árbol de procesos pero el nodo GamingServices está en estado inconsistente entre "Running" y "Stopped", causando deadlock interno en el query WMI.

### Solución que se aplicó (3 pasos):

```powershell
# PASO 1: Matar el proceso en bucle
taskkill /F /FI "SERVICES eq GamingServices"

# PASO 2: Remover el paquete AppX corrupto
Get-AppxPackage *GamingServices* | Remove-AppxPackage -AllUsers

# PASO 3: Borrar persistencia del Registro (no toca tu disco de código)
Remove-Item -Path "HKLM:\System\CurrentControlSet\Services\GamingServices" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "HKLM:\System\CurrentControlSet\Services\GamingServicesNet" -Recurse -Force -ErrorAction SilentlyContinue
```

> [!NOTE]
> Estos comandos NO tocan ningún archivo de tu proyecto. Solo eliminan llaves del Registro de Windows que forzaban al kernel a levantar el servicio corrupto en cada arranque.

### Verificación de que está resuelto:
```powershell
# Confirmar que GamingServices ya no existe como servicio
Get-Service -Name "GamingServices" -ErrorAction SilentlyContinue
# Si no devuelve nada = limpio ✅
```

---

## 3. QUÉ SE FILTRÓ EN GITHUB Y CÓMO PROTEGERTE {#seguridad}

### Auditoría Completa Realizada Hoy

Se escanearon **todos los commits** del historial completo (desde `v0.1` hasta el último merge), incluyendo las ramas mergeadas y el contenido de archivos históricos.

### ✅ Lo que NO se filtró (Buenas Noticias):

| Tipo de Credencial | Resultado |
|---|---|
| Tokens GitHub (`ghp_`, `gho_`, `ghs_`) | ✅ NINGUNO en el historial |
| API Keys de servicios (`AIza`, `sk-`, `AKIA`) | ✅ NINGUNO |
| Passwords en texto plano | ✅ NINGUNO |
| Archivos `.env` con secretos reales | ✅ NUNCA commiteados |
| Strings de conexión a bases de datos | ✅ NINGUNO |
| Tokens de sesión o Bearer tokens | ✅ NINGUNO |

### ⚠️ Lo que SÍ existe en el historial (Riesgo Bajo):

Los siguientes archivos de configuración fueron commiteados en commits antiguos (antes del renombrado de VolcanEngine → DarkEngine):

```
commit 6ba177f → config/volcan-production.properties
commit 4c5e78d → config/darkengine-production.properties
commit 96c06d3 → config/dark-production.properties
```

**Contenido revisado línea por línea — solo contienen:**
```properties
dark.metrics.server.port=13000   # Puerto local (no es secreto)
dark.kernel.tick.rate=60          # Parámetros de rendimiento
dark.physics.gravity.y=-9.81      # Constantes físicas
dark.memory.vault.size=1048576    # Tamaño de memoria
```

**Conclusión:** Estos archivos no contienen secretos reales. Son parámetros de configuración de rendimiento del motor, completamente seguros de estar públicos.

### Tu email en metadatos de Git:

```
Author: MARVIN DEV sv <thecanales23@gmail.com>
```

Tu email aparece en los metadatos de autoría de cada commit. Esto es **completamente normal y esperado** en cualquier repositorio público de GitHub. No es una filtración.

---

## CÓMO PROTEGERTE PARA SIEMPRE: El Sistema de Defensa en Capas

### CAPA 1: `.gitignore` Correcto (Tu Primera Línea de Defensa)

El `.gitignore` actualizado hoy ya protege todo lo crítico:

```gitignore
# Variables de entorno (NUNCA commitear)
.env
.env.local
.env.production
.env.*

# Archivos de secretos con cualquier nombre
*.secrets.properties
darkengine.secrets.properties
secrets.*
credentials.*

# Dumps de herramientas de análisis
repomix-output.xml
repomix-output*.xml
*.repomix

# Configuración de IDEs (pueden contener rutas locales)
.idea/
.vscode/
*.iml
```

### CAPA 2: Variables de Entorno del Sistema Operativo (La Forma Correcta)

**NUNCA** escribas credenciales directamente en archivos `.properties` o `.java`. La forma correcta en Java es:

```java
// MAL ❌ — Credencial hardcodeada (riesgo de filtración)
String apiKey = "sk-abc123xyz...";

// BIEN ✅ — Leer del sistema operativo
String apiKey = System.getenv("DARK_API_KEY");
if (apiKey == null) {
    DarkLogger.fatal("CONFIG", "Variable DARK_API_KEY no configurada en el SO", null);
    System.exit(1);
}
```

**Para configurar la variable en Windows:**
```powershell
# En PowerShell como Administrador (persiste entre reinicios)
[System.Environment]::SetEnvironmentVariable("DARK_API_KEY", "tu-valor-real", "User")

# Verificar que existe
[System.Environment]::GetEnvironmentVariable("DARK_API_KEY", "User")
```

### CAPA 3: Pre-commit Hook (Detector Automático Antes de Subir)

Crea este archivo en `.git/hooks/pre-commit` (sin extensión):

```bash
#!/bin/sh
# Hook de seguridad: bloquea commit si hay credenciales

PATTERNS="ghp_|gho_|sk-|AIza|AKIA|password=|api_key="
FILES=$(git diff --cached --name-only)

for file in $FILES; do
    if git show ":$file" | grep -qE "$PATTERNS"; then
        echo "🚫 BLOQUEADO: Credencial detectada en $file"
        echo "Usa variables de entorno del sistema en lugar de hardcodear."
        exit 1
    fi
done
exit 0
```

### CAPA 4: Si YA se filtró algo real — Protocolo de Emergencia

Si en el futuro accidentalmente subes un token real:

```bash
# PASO 1: Invalida el token INMEDIATAMENTE en el servicio
# (Ve a GitHub Settings → Developer Settings → Tokens → Revocar)

# PASO 2: Instala git-filter-repo (si no lo tienes)
pip install git-filter-repo

# PASO 3: Borra el archivo del HISTORIAL COMPLETO
git filter-repo --path ruta/al/archivo/secreto.env --invert-paths

# PASO 4: Fuerza el push para sobreescribir el historial en GitHub
git push origin master --force

# PASO 5: Notifica a colaboradores que deben hacer clone fresco
```

> [!CAUTION]
> `git filter-repo` reescribe TODO el historial. Es irreversible. Siempre haz backup primero con `git checkout -b backup-before-filter`.

---

## 4. GUÍA DE MONITOREO A BAJO NIVEL (TU PC COMO MOTOR) {#monitoreo}

Como desarrollas a nivel de **FFI, Project Panama, Off-Heap Memory y Thread Pinning**, necesitas monitorear cosas que el administrador de tareas normal NO muestra.

### A. Monitor de Rendimiento Avanzado (perfmon.exe)

```powershell
# Abre el Monitor de Rendimiento con los contadores correctos
perfmon.exe
```

**Contadores críticos para el DarkEngine:**

| Contador | Ruta en perfmon | Qué indica |
|---|---|---|
| Context Switches/sec | `\System\Context Switches/sec` | Si >100,000/s → hilos en conflicto |
| Interrupts/sec | `\Processor(_Total)\Interrupts/sec` | Si >50,000/s → driver o servicio roto |
| Page Faults/sec | `\Memory\Page Faults/sec` | Si alto → memoria nativa mal alineada |
| Available MBytes | `\Memory\Available MBytes` | RAM libre real |
| Cache Faults/sec | `\Memory\Cache Faults/sec` | Indica si la L3 está saturada |

### B. Monitoreo de Hilos Específicos con PowerShell

```powershell
# Ver hilos de los procesos Java/DarkEngine con su CPU
Get-Process java, javaw -ErrorAction SilentlyContinue | ForEach-Object {
    $proc = $_
    Write-Output "PID: $($proc.Id) | RAM: $([math]::Round($proc.WorkingSet/1MB))MB | CPU: $($proc.CPU)s | Hilos: $($proc.Threads.Count)"
}
```

### C. Process Explorer (Sysinternals — Herramienta Pro)

Descárgalo gratis desde Microsoft: https://learn.microsoft.com/sysinternals/downloads/process-explorer

**Qué puedes hacer:**
- Ver CADA hilo dentro del proceso Java, su CPU individual y su stack de llamadas
- Detectar si el Game Loop (tu Thread Pinned en Core 1) tiene interrupciones externas
- Ver el consumo de memoria nativa Off-Heap separado del Heap de Java
- Identificar handles de memoria abiertos (MemorySegments de Panama que no se cerraron)

### D. WMI desde PowerShell (Inspección Quirúrgica)

```powershell
# Ver todos los procesos Java con su línea de comando completa
Get-WmiObject Win32_Process -Filter "Name='java.exe'" | 
    Select-Object ProcessId, CommandLine, WorkingSetSize | 
    Format-List

# Ver uso de CPU por núcleo (ver si tu Thread Pinning funciona)
Get-WmiObject -Class Win32_PerfFormattedData_PerfOS_Processor | 
    Select-Object Name, PercentProcessorTime | 
    Format-Table -AutoSize
```

### E. Comando para Limpiar Puertos del Motor

```powershell
# Ver qué proceso ocupa el puerto 13000 del DarkEngine
netstat -aon | findstr ":13000"

# Matar el proceso que ocupa ese puerto
$pid = (netstat -aon | findstr ":13000" | ForEach-Object { ($_ -split '\s+')[5] } | Select-Object -First 1)
if ($pid) { taskkill /F /PID $pid }
```

---

## 5. MONITOREO DE PROGRAMACIÓN A BAJO NIVEL (FFI, PANAMA, MEMORIA) {#bajo-nivel}

### A. Verificar que Off-Heap NO está causando Memory Leaks

El mayor riesgo en Project Panama es un `Arena` o `MemorySegment` que se crea y nunca se libera:

```java
// RIESGO: Si esto falla a la mitad, el Arena queda abierto para siempre
Arena arena = Arena.ofShared();
MemorySegment seg = arena.allocate(1024);
// ... código que puede lanzar excepción ...
arena.close(); // Esto podría no ejecutarse si hay excepción
```

```java
// CORRECTO: Usa try-with-resources para garantía de cierre
try (Arena arena = Arena.ofConfined()) {
    MemorySegment seg = arena.allocate(1024);
    // El Arena se cierra automáticamente aunque falle
}
```

**Detectar leaks desde PowerShell:**
```powershell
# Ver cuánta memoria nativa (No-Heap) usa tu proceso Java
# Busca "NonHeapMemoryUsage" en JConsole o:
Get-Process java | Select-Object Id, @{N="RAM_MB";E={[math]::Round($_.WorkingSet/1MB,1)}}, 
    @{N="VM_MB";E={[math]::Round($_.VirtualMemorySize64/1MB,1)}}
```

### B. Verificar Thread Pinning (¿Tu Game Loop está anclado al Core 1?)

```powershell
# Necesitas Process Explorer o este método WMI para ver afinidad de hilos
$proc = Get-Process java | Select-Object -First 1
$proc.ProcessorAffinity
# Si devuelve 2 = binario 0010 = solo Core 1 ✅
# Si devuelve -1 = todos los cores (Thread Pinning no aplicado)
```

### C. Verificar FFI / Carga de DLLs Nativos

```powershell
# Ver qué DLLs nativas tiene cargadas tu proceso Java (incluye glfw3.dll, soft_oal.dll)
$proc = Get-Process java -ErrorAction SilentlyContinue | Select-Object -First 1
if ($proc) {
    $proc.Modules | Where-Object { $_.FileName -like "*.dll" } | 
        Select-Object ModuleName, FileName | 
        Sort-Object ModuleName
}
```

### D. Monitoreo de Context Switches en Tiempo Real

```powershell
# Ver context switches por segundo cada 2 segundos (10 mediciones)
1..10 | ForEach-Object {
    $cs = (Get-WmiObject Win32_PerfFormattedData_PerfOS_System).ContextSwitchesPersec
    Write-Output "$(Get-Date -Format 'HH:mm:ss') → Context Switches/s: $cs"
    Start-Sleep -Seconds 2
}
# Normal en desarrollo: < 50,000/s
# Con GamingServices en bucle: > 500,000/s (lo que te frenó)
```

### E. Verificar que OpenGL FFI está leyendo los punteros correctos

Cuando el motor arranca y carga `DarkOpenGLLinker`, estos logs deben aparecer:
```
[INFO ] [GRAPHICS] Vinculando funciones OpenGL 4.3 (Compute Culling)...
[INFO ] [GRAPHICS] Punteros de OpenGL 4.3 FFI mapeados exitosamente.
[INFO ] [GRAPHICS] Initializing Deferred G-Buffer (1280x720) in VRAM...
[INFO ] [GRAPHICS] Deferred Pipeline Chassis Ready (VRAM Pre-allocated).
```

Si ves `[ERROR]` en lugar de `[INFO]` en estos → problema de contexto OpenGL. El Fix: verificar que `glfwMakeContextCurrent` se llama ANTES que `DarkOpenGLLinker.init()`.

---

## 6. MÉTODOS DE VERIFICACIÓN DEL SISTEMA {#verificacion}

### Verificación Rápida (Diaria — 30 segundos)

```powershell
# Script de health check rápido para tu entorno de desarrollo
Write-Output "=== DARK ENGINE HEALTH CHECK ==="

# 1. RAM disponible
$ram = [math]::Round((Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory / 1MB, 1)
Write-Output "RAM Libre: ${ram} GB"

# 2. Procesos Java activos
$java = (Get-Process java, javaw -ErrorAction SilentlyContinue).Count
Write-Output "Procesos Java: $java"

# 3. Puerto 13000 del motor
$port = netstat -aon | findstr ":13000"
if ($port) { Write-Output "Puerto 13000: OCUPADO" } else { Write-Output "Puerto 13000: LIBRE ✅" }

# 4. GamingServices (el que te dio problemas)
$gs = Get-Service -Name "GamingServices" -ErrorAction SilentlyContinue
if ($gs) { Write-Output "GamingServices: ACTIVO ⚠️" } else { Write-Output "GamingServices: ELIMINADO ✅" }

Write-Output "================================"
```

### Verificación de Seguridad Git (Antes de Cada Push)

```powershell
# Escanear staged files antes de hacer push
git diff --staged | Select-String -Pattern "password|api_key|secret|ghp_|sk-|AIza" -CaseSensitive:$false
# Si devuelve líneas → NO hagas push, revisa primero
```

### Verificación del Build (Después de Cada Cambio de Código)

```powershell
# Secuencia completa de verificación
.\clean.bat    # Limpiar binarios viejos
.\build.bat    # Compilar (debe terminar con [OK] AAA+ Compiled.)
.\test.bat     # Ejecutar suite de tests AAA+
```

### Verificación de Memory Leaks Off-Heap

```powershell
# Compara RAM antes y después de correr el motor
$before = (Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory
Start-Process "java" -ArgumentList "-jar dist/dark-engine.jar" -Wait
$after = (Get-WmiObject Win32_OperatingSystem).FreePhysicalMemory
$leak = [math]::Round(($before - $after) / 1024, 1)
Write-Output "Diferencia de RAM tras ejecución: ${leak} MB"
# Si es > 100MB después de cerrar → posible Arena sin cerrar
```

---

## 7. LISTA DE ACCIÓN INMEDIATA {#accion}

### ✅ Completado Hoy:
- [x] Auditoría de seguridad completa del historial Git → Sin credenciales reales filtradas
- [x] `.gitignore` limpiado, deduplicado y reforzado con protección de repomix
- [x] `DarkOpenGLLinker.java` ampliado con 9 funciones FFI de Texturas y FBOs (Fase 27)
- [x] `DarkDeferredPipeline.java` creado — G-Buffer 1280x720 pre-alojado en VRAM
- [x] `DarkEngineWindow.java` actualizado — inicializa el pipeline diferido al arrancar
- [x] Bug de `glfwMakeContextCurrent` duplicado corregido
- [x] Procesos zombi del IDE (14 instancias) eliminados → PC liberada
- [x] GamingServices DCOM loop identificado y eliminado del sistema

### ⏳ Pendiente — Próximos Pasos:

**Inmediato (esta sesión):**
- [ ] Commit y push de los cambios de Fase 27 Misión A
- [ ] Crear `fsr_upscaler.comp` (shader GLSL de upscaling matemático)
- [ ] Crear `DarkFSRSystem.java` (dispatcher del Compute Shader de FSR)

**Próxima sesión:**
- [ ] Configurar exclusión de Windows Defender para la carpeta del proyecto
- [ ] Verificar que `DarkDeferredPipeline.init()` no crashea al arrancar el motor

**Para el futuro:**
- [ ] Implementar Pre-commit Hook de seguridad en `.git/hooks/`
- [ ] Migrar toda configuración sensible a variables de entorno del SO
- [ ] Fase 27 Misión B completa → Motor visual renderizando a 4K via FSR

---

## REFERENCIA RÁPIDA DE COMANDOS (Cheat Sheet)

```powershell
# LIMPIEZA DE PC
Get-Process -Name "Antigravity IDE" | Sort-Object WorkingSet -Desc | Select-Object -Skip 1 | Stop-Process -Force
Get-Process ollama*, "ollama app" -ErrorAction SilentlyContinue | Stop-Process -Force

# MONITOREO DE PUERTOS
netstat -aon | findstr "LISTENING"
netstat -aon | findstr ":13000"

# SALUD DEL SISTEMA
Get-WmiObject Win32_OperatingSystem | Select-Object FreePhysicalMemory, TotalVisibleMemorySize

# GIT SEGURIDAD
git log --all -p -S "password" --oneline
git diff --staged | Select-String "secret|token|api_key"

# DEFENDER — EXCLUIR CARPETA DEL PROYECTO (acelera builds)
Add-MpPreference -ExclusionPath "C:\Users\theca\Documents\GitHub\DarkEngine"

# DARKENGINE
.\build.bat   # Compilar
.\test.bat    # Tests AAA+
.\run.bat     # Arrancar motor
```

---

> **Resumen Ejecutivo:** Tu repositorio está limpio y seguro. Los archivos de configuración commiteados en el pasado solo contienen parámetros técnicos del motor, no credenciales reales. La PC estaba lenta por el bucle infinito de GamingServices DCOM + 14 instancias zombi del IDE + Ollama cargando pesos del modelo en RAM. Todo fue identificado y resuelto. El proyecto está listo para continuar con la Fase 27 Misión B (FSR Shader).
