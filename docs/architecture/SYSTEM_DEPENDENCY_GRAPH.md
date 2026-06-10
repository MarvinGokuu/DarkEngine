# SYSTEM_DEPENDENCY_GRAPH

**Subsistema**: Architecture / Traceability
**Tecnología**: Dependency Mapping
**Estado**: V2.0 Standard
**Autoridad**: System Architect

---

## 1. Core Domain (Kernel)

### `EngineKernel`
*   **Role**: Central Processing Unit
*   **Dependencies**:
    *   `TimeKeeper`: hardware-based timing (TSC).
    *   `SystemRegistry`: Active component registry.
    *   `StatePersistenceUnit`: Long-term state management.
    *   `EventRouter`: Internal event routing.
    *   `SectorMemoryManager`: Physical memory sector management.
    *   `KernelControlRegister`: State machine control.
    *   `MetricsPacker`: Telemetry packing.

### `BootSequence`
*   **Role**: Iitialization Protocol
*   **Dependencies**:
    *   `ExecutionIntegrity`: Environment verification.
    *   `AtomicBus`: Memory signature validation.

---

## 2. Transport Domain (Bus & Signals)

### `AtomicBus`
*   **Role**: High-Throughput Ring Buffer
*   **Dependencies**:
    *   `IEventBus`: Interface contract.
    *   `MemorySegment`: Foreign Memory API access.
    *   `VarHandle`: Hardware concurrency primitives.

### `SignalDispatcher`
*   **Role**: Signal Facade
*   **Dependencies**:
    *   `AtomicBus`: Underlying transport engine.
    *   `SignalPacker`: Binary packing utility.

### `SignalPacker`
*   **Role**: Bitwise Utility
*   **Dependencies**:
    *   **None** (Static Utility).
    *   CPU Native Instructions (Bitwise Ops).

---

## 3. State Domain (Memory)

### `StatePersistenceUnit`
*   **Role**: State Management
*   **Dependencies**:
    *   `Foreign Memory API`: Direct RAM access.
    *   `Arena`: Lifecycle management.

### `SectorMemoryManager`
*   **Role**: Physical Layout Manager
*   **Dependencies**:
    *   `Unsafe`: Raw memory allocation.

---

## 4. System External Dependencies

*   **Java 25 (LTS)**: Runtime environment.
*   **GraalVM Native Image**: AOT Compilation.
*   **Project Panama**: Foreign Function & Memory API.
*   **Vector API**: SIMD Instructions (AVX-512).

---

**Versión**: 2.0
**Estado**: VIGENTE
**Autoridad**: System Architect
