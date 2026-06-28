# 🌌 DarkEngine — High-Performance Lock-Free Java Runtime

**Autoridad**: Chief Executive Officer / Kernel Architect  
**Tecnología Core**: Java 25 (Project Panama, Vector API, Loom)  
**Estado**: Production Ready (AAA+ Certified)  

> *el hardware barato puede entregar latencias extremas si aplicas Mechanical Sympathy absoluta.*

---

## 1. VISION Y DOMINIO DEL HARDWARE

El desarrollo de runtimes tradicionales está plagado de cuellos de botella (Garbage Collectors, OS Schedulers, Thread Locks). **DarkEngine somete al hardware:**

1.  **Cero Pausas GC**: Todo el estado reside en memoria nativa (Off-Heap) vía **Project Panama**. 
2.  **Cero Locks (Vyukov MPMC)**: La concurrencia se logra con colas en anillo (Ring Buffers) Lock-Free MPMC para el despacho de tareas del grafo acíclico dirigido (DAG).
3.  **Ancho de Banda SIMD**: Matemáticas vectorizadas vía `jdk.incubator.vector` (AVX-512) para anchos de banda procesados superiores a `4.17 GB/s`.
4.  **Anti-False Sharing**: Aislamiento y padding de estructuras críticas a 64 bytes para empatizar perfectamente con las *L1 Cache Lines*.
5.  **Renderizado FFI (Phase 9)**: Chasis nativo usando GLFW y Dear ImGui conectados por Project Panama, logrando 0ms de Input Lag.
6.  **GPU-Driven Compute (Phase 19 & Phase 3)**: Descarte masivo de geometría (Culling) y **Spatial Hashing** 100% en VRAM vía Compute Shaders y SSBOs AZDO con `atomicExchange`.
7.  **Streaming Zero-Copy (Phase 3)**: Ingestión de memoria desde disco directo a la GPU. Los *DarkAssets* saltan directo a un *Pixel Unpack Buffer* (PBO) de 64MB mapeado de forma persistente.
8.  **Clustered Deferred Shading (Phase 29)**: G-Buffers, *Light Culling* por Compute Shaders y partición espacial en clústeres. Cálculo de sombras CSM exactas mediante inversión matricial pura para erradicar el *Shadow Shimmering*.
9.  **0 Deuda Técnica y VRAM Safe (Phase 28)**: Destrucción de recursos gráficos controlada.
10. **Zero-GC Telemetry (Phase 1)**: Reemplazo absoluto del `HttpServer` por un Gateway NIO Asíncrono. Parseo ASCII directo a *ByteBuffers* precompilados sin crear objetos `String`.
11. **DAG Task Dispatcher (Phase 4)**: Destrucción de las barreras globales multicore. Las dependencias se resuelven atómicamente, despertando hilos elásticamente vía `LockSupport`.

Para entender la motivación y cómo planeamos revolucionar el mercado a $1/mes, lee:
 **[EL MANIFIESTO DARK ENGINE](docs/vision/DARK_ENGINE_MANIFESTO.md)**

---

## 2. ARQUITECTURA TÉCNICA (MECHANICAL SYMPATHY)

No existe un "sistema orquestador" que ahogue el rendimiento con overhead de gestión. Toda la base de código se rige por el determinismo asíncrono y paralelismo puro.

### explicativo didáctico!
Si quieres entender cómo implementamos todo esto usando Java 25, debes estudiar nuestras especificaciones de arquitectura Core. Aquí reside el conocimiento clasificado del motor:

1. **[MECHANICAL SYMPATHY CORE](docs/architecture/MECHANICAL_SYMPATHY_CORE.md)**: Cómo destruir el "False Sharing", alinear estructuras de memoria y operar atómicamente con VarHandles.
2. **[SYSTEM ARCHITECTURE](docs/architecture/SYSTEM_ARCHITECTURE.md)**: El flujo de ejecución del Kernel y el diseño modular libre de bloqueos.
3. **[CODE_DEBT_DETAILED_AUDIT](docs/reports/CODE_DEBT_DETAILED_AUDIT.md)**: La auditoría completa y detallada de deuda técnica de código, cuellos de botella de hardware, y plan de escalamiento a Java 26 / Valhalla.

---

## 3. COMPILACIÓN Y EJECUCIÓN (BOOTSTRAPPING)

### Prerrequisitos de Compilación
*   **JDK**: Oracle GraalVM 25 / OpenJDK 25 (módulos incubadora habilitados).
*   **OS**: Windows 11 / Linux Kernel 6.x.

### Operación Básica
Desde la terminal, utiliza los scripts provistos en la raíz del proyecto para comandar el runtime:

```bash
# 1. Compilación brutal y enlazado
.\build.bat

# 2. Verificación de todas las pruebas AAA+ y benchmarks
.\scripts\test.bat

# 3. Limpiar binarios y liberar memoria de procesos "zombies"
.\clean.bat

# 4. Lanzar el motor en segundo plano de manera autónoma
.\run.bat
```

> ⚠️ **Nota de Arquitecto**: Al modificar código, respeta los estándares AAA+. Nunca inyectes clases `new String()` ni reservas de Heap en el hot-path del motor. Todo se ejecuta `Off-Heap`.

> ℹ️ **Nota sobre ImGui (UI del Editor)**: Es normal ver el mensaje `[ERROR] [IMGUI] lib/cimgui.dll NOT FOUND` en los logs. Esto simplemente indica que la interfaz visual nativa del editor está deshabilitada intencionalmente en producción, permitiendo que el motor corra en modo puramente *headless* o "juego final".

---

**Licencia**: [GNU Lesser General Public License v3.0](LICENSE)  
# 🌌 DarkEngine — High-Performance Lock-Free Java Runtime

**Autoridad**: Chief Executive Officer / Kernel Architect  
**Tecnología Core**: Java 25 (Project Panama, Vector API, Loom)  
**Estado**: Production Ready (AAA+ Certified)  

> *el hardware barato puede entregar latencias extremas si aplicas Mechanical Sympathy absoluta.*

---

## 1. VISION Y DOMINIO DEL HARDWARE

El desarrollo de runtimes tradicionales está plagado de cuellos de botella (Garbage Collectors, OS Schedulers, Thread Locks). **DarkEngine somete al hardware:**

1.  **Cero Pausas GC**: Todo el estado reside en memoria nativa (Off-Heap) vía **Project Panama**. 
2.  **Cero Locks (Vyukov MPMC)**: La concurrencia se logra con colas en anillo (Ring Buffers) Lock-Free MPMC para el despacho de tareas del grafo acíclico dirigido (DAG).
3.  **Ancho de Banda SIMD**: Matemáticas vectorizadas vía `jdk.incubator.vector` (AVX-512) para anchos de banda procesados superiores a `4.17 GB/s`.
4.  **Anti-False Sharing**: Aislamiento y padding de estructuras críticas a 64 bytes para empatizar perfectamente con las *L1 Cache Lines*.
5.  **Renderizado FFI (Phase 9)**: Chasis nativo usando GLFW y Dear ImGui conectados por Project Panama, logrando 0ms de Input Lag.
6.  **GPU-Driven Compute (Phase 19 & Phase 3)**: Descarte masivo de geometría (Culling) y **Spatial Hashing** 100% en VRAM vía Compute Shaders y SSBOs AZDO con `atomicExchange`.
7.  **Streaming Zero-Copy (Phase 3)**: Ingestión de memoria desde disco directo a la GPU. Los *DarkAssets* saltan directo a un *Pixel Unpack Buffer* (PBO) de 64MB mapeado de forma persistente.
8.  **Clustered Deferred Shading (Phase 29)**: G-Buffers, *Light Culling* por Compute Shaders y partición espacial en clústeres. Cálculo de sombras CSM exactas mediante inversión matricial pura para erradicar el *Shadow Shimmering*.
9.  **0 Deuda Técnica y VRAM Safe (Phase 28)**: Destrucción de recursos gráficos controlada.
10. **Zero-GC Telemetry (Phase 1)**: Reemplazo absoluto del `HttpServer` por un Gateway NIO Asíncrono. Parseo ASCII directo a *ByteBuffers* precompilados sin crear objetos `String`.
11. **DAG Task Dispatcher (Phase 4)**: Destrucción de las barreras globales multicore. Las dependencias se resuelven atómicamente, despertando hilos elásticamente vía `LockSupport`.

Para entender la motivación y cómo planeamos revolucionar el mercado a $1/mes, lee:
 **[EL MANIFIESTO DARK ENGINE](docs/vision/DARK_ENGINE_MANIFESTO.md)**

---

## 2. ARQUITECTURA TÉCNICA (MECHANICAL SYMPATHY)

No existe un "sistema orquestador" que ahogue el rendimiento con overhead de gestión. Toda la base de código se rige por el determinismo asíncrono y paralelismo puro.

### explicativo didáctico!
Si quieres entender cómo implementamos todo esto usando Java 25, debes estudiar nuestras especificaciones de arquitectura Core. Aquí reside el conocimiento clasificado del motor:

1. **[MECHANICAL SYMPATHY CORE](docs/architecture/MECHANICAL_SYMPATHY_CORE.md)**: Cómo destruir el "False Sharing", alinear estructuras de memoria y operar atómicamente con VarHandles.
2. **[SYSTEM ARCHITECTURE](docs/architecture/SYSTEM_ARCHITECTURE.md)**: El flujo de ejecución del Kernel y el diseño modular libre de bloqueos.
3. **[CODE_DEBT_DETAILED_AUDIT](docs/reports/CODE_DEBT_DETAILED_AUDIT.md)**: La auditoría completa y detallada de deuda técnica de código, cuellos de botella de hardware, y plan de escalamiento a Java 26 / Valhalla.

---

## 3. COMPILACIÓN Y EJECUCIÓN (BOOTSTRAPPING)

### Prerrequisitos de Compilación
*   **JDK**: Oracle GraalVM 25 / OpenJDK 25 (módulos incubadora habilitados).
*   **OS**: Windows 11 / Linux Kernel 6.x.

### Operación Básica
Desde la terminal, utiliza los scripts provistos en la raíz del proyecto para comandar el runtime:

```bash
# 1. Compilación brutal y enlazado
.\build.bat

# 2. Verificación de todas las pruebas AAA+ y benchmarks
.\scripts\test.bat

# 3. Limpiar binarios y liberar memoria de procesos "zombies"
.\clean.bat

# 4. Lanzar el motor en segundo plano de manera autónoma
.\run.bat
```

> ⚠️ **Nota de Arquitecto**: Al modificar código, respeta los estándares AAA+. Nunca inyectes clases `new String()` ni reservas de Heap en el hot-path del motor. Todo se ejecuta `Off-Heap`.

> ℹ️ **Nota sobre ImGui (UI del Editor)**: Es normal ver el mensaje `[ERROR] [IMGUI] lib/cimgui.dll NOT FOUND` en los logs. Esto simplemente indica que la interfaz visual nativa del editor está deshabilitada intencionalmente en producción, permitiendo que el motor corra en modo puramente *headless* o "juego final".

---

**Licencia**: [GNU Lesser General Public License v3.0](LICENSE)  
**Versión**: v3.0 (Pure Mechanical Sympathy Edition)

---

## 4. TAREAS PENDIENTES (PRÓXIMA SESIÓN)

* [x] **DarkTaskDispatcher (DAG)**: Implementado un despachador de tareas asíncrono basado en un *Vyukov Bounded MPMC Ring Buffer* lock-free. Elimina las barreras de capa de Kahn, aumentando la utilización del CPU en máquinas multinúcleo.
* [x] **DarkMetricsServer (Zero-GC Telemetry)**: Reescribimos el servidor HTTP NIO. Ahora emplea búferes pre-asignados, `ThreadLocal` scratchpads y escritura de dígitos ASCII crudos para un 0% de allocations por request.
* [x] **Cascaded Shadow Mapping (CSM) Exacto**: `DarkShadowSystem` y `DarkMath` realizan inversión matricial pura `inverse(ViewProj)` con snapshots alias-safe para eliminar definitivamente el temblor de sombras.
* [x] **GPU Spatial Hashing**: El cálculo espacial (Broadphase) ha migrado completamente a VRAM usando `radix_sort.comp` interactuando con los SSBO AZDO para inyectar IDs en la grilla mediante `atomicExchange`.
* [x] **Purga de Sistemas Legacy (DOD)**: Sistemas obsoletos basados en objetos (`AudioSystem`, `MovementSystem`, `RenderSystem`, `SpriteSystem`) fueron completamente eliminados, consolidando el motor 100% sobre las versiones Data-Oriented nativas (`Dark*System`).

El motor se encuentra en un estado arquitectónico **AAA+** con GitFlow estandarizado en `master`, `develop` y `release/*`. El enfoque total actual es limpiar la Fase 1.
