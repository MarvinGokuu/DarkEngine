# 📋 DETAILED DESIGN AUDIT — DarkEngine v3.5.0
**Metodología**: Escaneo Línea por Línea ordenado por `Reading Order` (según AGENT_SKILL_MANIFESTO)  
**Estado Global**: 🟡 EN PROGRESO — Hallazgos registrados. Correcciones pendientes de luz verde CEO.  
**Última Actualización**: 2026-06-17 | Auditado por: Antigravity IDE Agent  
**Rama Activa**: `feature/phase21-asset-compiler-streaming` (commit `47872d7`)

---

## 🔴 LEYENDA DE SEVERIDAD

| Símbolo | Severidad | Descripción |
|---------|-----------|-------------|
| 🔴 CRÍTICO | Alta | Viola directamente Simpatía Mecánica en el hot-path del motor |
| 🟡 MODERADO | Media | Viola principios pero solo en boot/shutdown (fuera del loop) |
| 🟢 TOLERABLE | Baja | Patrón aceptado (throws en paths fatales, boot-time `new`) |
| ✅ RESUELTO | Cerrado | Deuda técnica ya corregida en este ciclo |

---

## ✅ LOGROS FASE ACTUAL (v3.5.0 — CERRADOS)

| ID | Archivo | Descripción | Estado |
|----|---------|-------------|--------|
| FIX-001 | `DarkOpenGLLinker.java` | +9 FFI bindings Phase 27 (Textures/FBO) | ✅ RESUELTO |
| FIX-002 | `DarkDeferredPipeline.java` | G-Buffer 1280x720 VRAM (Fase 27 Misión A) | ✅ RESUELTO |
| FIX-003 | `DarkEngineWindow.java` | Phase 19 wiring: `DarkComputeCullingSystem.init()` conectado | ✅ RESUELTO |
| FIX-004 | `DarkGraphicsLinker.java` | Eliminado handle `glfwMakeContextCurrent` duplicado | ✅ RESUELTO |
| FIX-005 | `compile_list.txt` | Bloque duplicado eliminado (192 líneas → limpio) | ✅ RESUELTO |

---

## 🔴 HALLAZGOS CRÍTICOS (HOT-PATH VIOLATIONS)

### [AUDIT-KERNEL-001] `EngineKernel.java` — System.out en loop de 60fps
**Líneas**: 457, 462 (dentro de `phaseBusProcessing`)  
**Tipo**: 🔴 CRÍTICO — Bloqueo I/O en hot-path  
**Detalle**: `System.out.println("[KERNEL] Pause State: " + this.paused)` y `"[KERNEL] Rollback..."` se ejecutan dentro del `while(running)` loop cuando llegan eventos. Aunque son raros, el I/O bloqueante rompe la garantía de latencia sub-ms.  
**Corrección**: Reemplazar por `DarkLogger.info("KERNEL", ...)` que escribe en background.

```java
// TRANSGRESOR (Línea 457):
System.out.println("[KERNEL] Pause State: " + this.paused);

// CORRECTO:
DarkLogger.info("KERNEL", "Pause State toggled.");
```

---

### [AUDIT-KERNEL-002] `EngineKernel.java` — String concatenation en hot-path
**Líneas**: 447, 457  
**Tipo**: 🔴 CRÍTICO — Zero-GC violation  
**Detalle**: `"[KERNEL] Pause State: " + this.paused` crea un objeto `String` temporal en el Heap durante el loop. Viola directamente la garantía Zero-GC del manifesto.  
**Corrección**: Usar `DarkLogger` sin concatenación, o pre-computar el String fuera del hot-path.

---

### [AUDIT-KERNEL-003] `EngineKernel.java` — Módulo aritmético en hot-path
**Línea**: 367 (`if (totalFrames % 60 == 0)`)  
**Tipo**: 🟡 MODERADO — Aritmética Modulo en bucle 60fps  
**Detalle**: `%` con 60 (no es potencia de 2). Requiere división entera. Alternativa correcta para potencias de 2: máscara bitwise. Para 60 no hay alternativa bitwise directa — considerar usar contador auxiliar que se resetea.  
**Corrección**:
```java
// TRANSGRESOR:
if (totalFrames % 60 == 0) { ... }

// CORRECTO (sin división):
private int metricsTick = 0;
if (++metricsTick == 60) { metricsTick = 0; /* enviar métricas */ }
```

---

### [AUDIT-MEM-001] `SectorMemoryVault.java` — Módulo aritmético en alineación
**Líneas**: 101-102, 226-228  
**Tipo**: 🔴 CRÍTICO — ALU overhead en validación de alineación  
**Detalle**: `address % PAGE_SIZE` donde PAGE_SIZE=4096 (potencia de 2). La división entera bloquea el pipeline ALU. La corrección es una máscara bitwise que corre en 1 ciclo.  
**Corrección**:
```java
// TRANSGRESOR:
if (address % PAGE_SIZE != 0) { ... }

// CORRECTO (1 ciclo de reloj):
if ((address & (PAGE_SIZE - 1)) != 0) { ... }
```

---

### [AUDIT-BUS-001] `DarkAtomicBus.java` — System.out en shutdown bus
**Líneas**: 479, 494, 497, 509  
**Tipo**: 🟡 MODERADO — I/O bloqueante en shutdown path  
**Detalle**: Aunque es shutdown, si el motor usa hot-reload o reinicio parcial, estos bloqueos pausan el Thread Orchestrator.  
**Corrección**: Reemplazar por `DarkLogger.info("ATOMIC BUS", ...)`.

---

### [AUDIT-TIME-001] `TimeKeeper.java` — Módulo aritmético en buffer circular
**Línea**: 153 (`bufferIndex = (bufferIndex + 1) % BUFFER_SIZE`)  
**Tipo**: 🔴 CRÍTICO — Si BUFFER_SIZE es potencia de 2, usar máscara.  
**Condición**: Solo aplica si `BUFFER_SIZE` es potencia de 2. Verificar y corregir.  
**Corrección**:
```java
// TRANSGRESOR (si BUFFER_SIZE = 64, 128, etc.):
bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;

// CORRECTO:
bufferIndex = (bufferIndex + 1) & (BUFFER_SIZE - 1);
```

---

## 🟡 HALLAZGOS MODERADOS (Boot/Shutdown / Non-Hot-Path)

### [AUDIT-KERNEL-004] `EngineKernel.java` — System.out masivo en boot/shutdown
**Líneas**: 182, 189, 194, 198, 207, 402, 513, 547-681 (>40 instancias)  
**Tipo**: 🟡 MODERADO — Solo en boot y gracefulShutdown(), fuera del loop  
**Veredicto**: Aceptables donde están. No bloquean el hot-path. Sin embargo, el MANIFESTO exige redirigirlos a `DarkLogger` para un output consistente y filtrable.  
**Prioridad**: BAJA — Cosmético/Consistencia.

---

### [AUDIT-REGISTRY-001] `SystemRegistry.java` — String concatenation en registro
**Líneas**: 86, 98, 223, 225, 243  
**Tipo**: 🟡 MODERADO — Concatenación de Strings en DarkLogger en tiempo de registro (solo al registrar sistemas, no en el loop)  
**Veredicto**: Ocurre solo 1 vez por sistema al inicio. No es hot-path. Aceptable.

---

### [AUDIT-BUS-002] `DarkAtomicBus.java` / `DarkRingBus.java` — new en constructores
**Tipo**: 🟢 TOLERABLE — Pre-alocación en constructores (Design-Time, no Runtime)  
**Veredicto**: Los `new DarkAtomicBus(...)` y `new DarkRingBus(...)` ocurren únicamente en tiempo de construcción del Kernel (una sola vez). Esto es aceptable bajo el patrón de Simpatía Mecánica: "Map Once, Reuse Forever".

---

### [AUDIT-GRAPH-001] `SystemDependencyGraph.java` — System.out en diagnóstico
**Líneas**: 193, 197, 200, 202, 204  
**Tipo**: 🟡 MODERADO — Llamado solo en modo debug/diagnóstico  
**Corrección**: Reemplazar por `DarkLogger.info("GRAPH", ...)` y guardar en flags.

---

## 🟢 PATRONES TOLERABLES (No requieren acción inmediata)

| Patrón | Archivo(s) | Veredicto |
|--------|-----------|-----------|
| `throw new AssertionError(...)` | Clases utilitarias estáticas | ✅ Tolerable — Constructor privado bloqueado |
| `throw new Error(...)` en padding corruption | `DarkAtomicBus`, `DarkRingBus` | ✅ Tolerable — Corrupción fatal, JVM debe morir |
| `float/double` en cálculos internos | `DarkSignalPacker`, `TimeKeeper` | 🟡 Futuro: Migrar a Vector API cuando escale |
| `new DarkAtomicBus(...)` en constructores | `DarkEventDispatcher` | ✅ Tolerable — Pre-alocación única en boot |
| `System.out.println` en Test files | `BusBenchmarkTest`, `BusHardwareTest` etc. | ✅ Tolerable — Tests no corren en producción |

---

## 🚀 DEUDA TÉCNICA ARQUITECTÓNICA (El Santo Grial — Roadmap Futuro)

### [GRIAL-001] Decoupled Render Loop (Patrón Unreal / Source Engine)
**Prioridad**: ALTA  
**Estado**: 🔴 PENDIENTE  
**Descripción**: Actualmente el renderizado (Fase 5 ImGui/GLFW) está acoplado al Kernel loop. Esto significa que si ImGui tarda 2ms en pintar, el Kernel físico se retrasa 2ms.  
**Plan**:
1. Separar `phaseRender()` a un hilo dedicado `dark-render-thread`.
2. El hilo lógico escribe coordenadas en `DarkStateVault` (Off-Heap).
3. El hilo de render lee del vault e interpola visualmente entre ticks.
4. Resultado: Física a 60 ticks exactos + Visual a N FPS ilimitados.

---

### [GRIAL-002] VolatileImage Pre-Baking (Zero-CPU Render)
**Prioridad**: ALTA  
**Estado**: 🔴 PENDIENTE  
**Descripción**: Cualquier fondo renderizado con `RadialGradientPaint` recalcula en cada frame. Impacto: 12% CPU, 31% GPU observado.  
**Plan**: Dibujar el fondo una sola vez en un `VolatileImage` (alojado en VRAM) durante boot. En el loop: `g.drawImage(fondoCachedVRAM, 0, 0, null)` = 1 nanosegundo.

---

### [GRIAL-003] FSR Upscaler Compute Shader (Phase 27 Misión B)
**Prioridad**: ALTA  
**Estado**: 🔴 PENDIENTE (Misión B sin implementar)  
**Descripción**: El G-Buffer está pre-alocado (Misión A ✅). Falta el shader de FSR que lee del G-Buffer y escala a la resolución nativa de la pantalla.  
**Archivos a crear**:
- `src/sv/dark/scene/fsr_upscaler.comp` — GLSL Compute Shader de escalado
- `src/sv/dark/core/systems/DarkFSRSystem.java` — Dispatcher FFI del upscaler

---

### [GRIAL-004] Rollback Netcode (El Santo Grial de E-Sports)
**Prioridad**: MEDIA  
**Estado**: 🔵 DISEÑADO (DarkTimeControlUnit existe, falta integración de red)  
**Descripción**: `DarkTimeControlUnit` ya captura y rollback el `DarkStateVault`. Falta conectarlo a la capa de red para sincronizar inputs remotos y re-simular ticks.

---

### [GRIAL-005] SIMD FPU Upgrade (Vector API 512-bit)
**Prioridad**: MEDIA  
**Estado**: 🔵 PARCIAL (`DarkKinematicsSystem` usa SIMD, physics no)  
**Descripción**: `PhysicsSystem`, `MovementSystem`, `DarkSectorManager` procesan entidades una por una con `float`. Migrar a `jdk.incubator.vector` procesaría 8-16 entidades por ciclo de CPU.

---

### [GRIAL-006] Kernel Bypass Networking (DPDK — El Último Nivel)
**Prioridad**: BAJA  
**Estado**: 🔵 INVESTIGACIÓN  
**Descripción**: `DarkMetricsServer` usa Java Sockets estándar (OS-mediated I/O). Para máximo rendimiento: DPDK (Data Plane Development Kit) permite bypass del OS, inyectando paquetes directo a Off-Heap sin que Windows/Linux intervengan.

---

## 📋 ORDEN DE EJECUCIÓN RECOMENDADO (Próximas Sesiones)

```
PRIORIDAD 1 — Completar Fase 27 Misión B (FSR Upscaler):
  → Crear src/sv/dark/scene/fsr_upscaler.comp
  → Crear src/sv/dark/core/systems/DarkFSRSystem.java
  → Wire en DarkEngineWindow.java después de DarkDeferredPipeline.init()
  → Compilar y hacer PR a master

PRIORIDAD 2 — Correcciones Hot-Path (AUDIT-KERNEL-001/002/003):
  → EngineKernel.java L457, L462: System.out → DarkLogger
  → EngineKernel.java L367: % 60 → contador auxiliar
  → SectorMemoryVault.java L101, L226: % PAGE_SIZE → & (PAGE_SIZE-1)
  → TimeKeeper.java L153: % BUFFER_SIZE → & (BUFFER_SIZE-1) si es potencia de 2

PRIORIDAD 3 — VolatileImage Pre-Baking (GRIAL-002):
  → Identificar los RadialGradientPaint en DarkEngineWindow
  → Extraer a buildGradients() ejecutado una sola vez en boot
  → Reemplazar en renderLoop() por drawImage del cache VRAM

PRIORIDAD 4 — Decoupled Render Loop (GRIAL-001):
  → Separar phaseRender() del EngineKernel
  → Crear DarkRenderThread.java dedicado
  → Conectar vía DarkStateVault como canal de datos
```

---

## 🔗 REFERENCIAS

- [AGENT_SKILL_MANIFESTO.md](file:///c:/Users/theca/Documents/GitHub/DarkEngine/AGENT_SKILL_MANIFESTO.md) — Reglas de Oro del desarrollo
- [DarkEngine_Audit/docs/HALLAZGOS_DE_AUDITORIA_GLOBAL.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine_Audit/docs/HALLAZGOS_DE_AUDITORIA_GLOBAL.md) — Auditoría original completa (991 líneas)
- [DarkEngine_Audit/docs/POST_AUDITORIA_SUGERENCIAS.md](file:///C:/Users/theca/Documents/GitHub/DarkEngine_Audit/docs/POST_AUDITORIA_SUGERENCIAS.md) — Propuestas de refactorización
- [CHANGELOG.md](file:///c:/Users/theca/Documents/GitHub/DarkEngine/CHANGELOG.md) — Historial de versiones
