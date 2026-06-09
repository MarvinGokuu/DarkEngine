# MASTER_PLAN_V2

**Subsistema**: System Architecture
**Tecnología**: Java 25 / Vector API / Panama
**Estado**: Implementation Phase
**Integridad**: 98.5%

---

## 1. Manifiesto de Arquitectura del Sistema

El objetivo es implementar un Runtime de Alto Rendimiento, escindido del sistema operativo anfitrión (Host-Agnostic), que optimice la ejecución sobre el hardware disponible.

### 1.1. Principios de Diseño
1.  **Determinismo**: `Input` + `floatToRawIntBits` $\rightarrow$ `Output` constante.
2.  **Wait-Free Execution**: Topología trifásica para eliminar bloqueos de CPU.
3.  **Memory Layout**: Estructuras de datos alineadas a cache-line (64 bytes). Uso exclusivo de `MemorySegment` y vectores primitivos.
4.  **Signal Integrity**: Tratamiento de datos corruptos como señales de entropía, no excepciones de interrupción.

---

## 2. Topología Trifásica (System Core)

Ecuación de estado del sistema:
$$ \Phi(t) = \vec{A}_{t-1} + \vec{E}_{t} + \vec{P}_{t+1} $$

### Definición de Roles de Hardware

| Contexto | Rol Técnico | Responsabilidad | Ciclo |
| :--- | :--- | :--- | :--- |
| **CORE 1** | **Supervisor Context** | Auditoría (N-1), Dispatch (N+1). Control Flow. | 60 Hz |
| **CORE 2-N** | **SIMD Workers** | Cálculo vectorial y transformación de matrices. | Asíncrono |
| **MEMORIA** | **Off-heap Region** | Persistencia alineada a 64 bytes. | VarHandle Access |

---

## 3. Estado del Roadmap Técnico

**Progreso Global del Runtime: ~66%**

### Phase 1: Infrastructure & Memory Architecture (Completed — 100%)
*   **Métrica**: Boot latency < 0.15ms (Cold) / < 0.07ms (Warm), Event throughput > 185M/s, Bus latency ~23.35ns.
*   **Componentes**: `UltraFastBootSequence`, `SectorMemoryVault`, `KernelControlRegister`, `BusSymmetryValidator`, `DarkRingBus`, `DarkAtomicBus`, `ThreadPinning` (Afinidad anclada a Core).
*   **Estado**: Lanzado y estable bajo el namespace `sv.dark` con Licencia Apache 2.0.

### Phase 2: Visual Telemetry, GUI & Logging (In Progress — 95%)
*   **Métrica**: Render loop estable a 60 FPS, logging asíncrono con 0ns de bloqueo en hot-path.
*   **Componentes**: `DarkEngineWindow` (Centrada con decoraciones nativas), `DarkMetricsServer`, `AsyncLogWriter`, `EngineStateChannel`, `tools/visual-observer/`.
*   **Pendiente**:
    - [ ] **Optimización del Cliente (`DarkMetricsClient.js`)**: Uso exclusivo de `TypedArrays` nativos en JS para evitar allocations de Garbage Collector en el navegador.
    - [ ] **Sincronización de Tipos**: Validación estricta del traspaso de IDs de eventos de 64 bits (Java `long` $\leftrightarrow$ JS `BigInt`).

### Phase 3: Integrity & Physics Alignment (Planned — 30%)
*   **Objetivo**: Validación de integridad de datos en vuelo libre de bifurcaciones (branchless) y control de física espacial determinista.
*   **Componentes**:
    - [ ] `MidAirByteAligner.java`: Alineación de assets binarios en RAM nativa mediante máscaras de bits (`address & ~63`) a velocidad del bus físico.
    - [ ] `SovereignSupervisor.java`: Auditoría activa de integridad de frame y predicción de carga (`predictNextLoad()`) aislada en el Core 1.
    - [ ] `SovereignSpaceMath.java` (Migración de `SpaceMath.java`): Validación matemática de fronteras de sectores utilizando instrucciones SIMD de la Vector API de Java 25.
    - [ ] `EntropyMasking`: Generador de números pseudoaleatorios (PRNG) libre de bloqueos basado en máscaras de entropía para física reproducible.

### Phase 4: Distributed Intelligence & Balancing (Planned — 0%)
*   **Objetivo**: Optimización adaptativa del paralelismo y minimización de cache flushes.
*   **Componentes**:
    - [ ] **Branch Prediction Hinting**: Optimización de patrones de bifurcación de instrucciones para favorecer la inlining del compilador JIT (C2).
    - [ ] **Dynamic Work Balancing**: Distribución dinámica de hilos de trabajo SIMD en `ParallelSystemExecutor` basada en la latencia histórica del frame.

---

## 4. Inventario de Componentes Críticos

### 4.1. Telemetry Subsystem
**Dependencia**: `AdminController` $\rightarrow$ `VisualObserver`

*   `DarkMetricsClient.js`: Cliente HTTP determinista. Uso de `TypedArrays` para evitar GC en el render loop.
*   `VisualObserver.html`: Dashboard de instrumentación técnica.

### 4.2. Supervisor Subsystem
**Dependencia**: `EngineKernel` $\rightarrow$ `SovereignSupervisor`

*   `SovereignSupervisor.java`: Auditoría de integridad de frame. Thread Affinity a Core 1.
    *   Métodos: `auditFrameIntegrity()`, `predictNextLoad()`.

### 4.3. Vector Calculation Subsystem
**Dependencia**: `SovereignSectorMap` $\rightarrow$ `VectorCalculationWorker`

*   `VectorCalculationWorker.java`: Procesamiento paralelo SIMD.
    *   Tecnología: Java 25 Vector API (`FloatVector.SPECIES_256`).
    *   Capacidad: Procesamiento de entidades en batch.

### 4.4. I/O Alignment Subsystem
**Dependencia**: `SectorMemoryVault` $\rightarrow$ `MidAirByteAligner`

*   `MidAirByteAligner.java`: Ingesta y alineación de datos raw. Lógica branchless.

---

## 5. Estándares de Codificación (Sintaxis Estricta)

1.  **Nomenclatura**:
    *   Descriptiva y técnica: `headShield_L1`, `metric_frameVoltage`, `sector_A_vault`.
    *   Evitar nombres genéricos (`temp`, `data`, `x`).

2.  **Control Flow**:
    *   Hot-Path libre de excepciones (`try-catch` prohibido).
    *   Uso de aritmética de bits para validación: `errorCount += (result ^ expected) & 1`.

3.  **Documentación**:
    *   Explicitar impacto en hardware (Alignment, False Sharing, Pipeline Flush).
    *   Ejemplo: `// ALIGNMENT: 64 bytes to prevent False Sharing on L1`

---

**Estado**: GO
**Autoridad**: System Architect
