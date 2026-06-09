# HPC_CODING_STANDARD

**Subsistema**: Engineering Standards
**Tecnología**: Java 25 (Panama, Vector, Loom)
**Estado**: Mandatory Specification
**Autoridad**: System Architect

---

## 1. Protocolo de Documentación en Código

### 1.1 Bloques de Especificación
Cada componente crítico debe incluir documentación técnica centrada en el hardware.

```java
// BARRIER SEMANTICS: Acquire/Release
//
// CONTEXT:
// VarHandles provide ordered memory access without monitor locks,
// achieving ~150ns latency.
//
// MECHANICS (Acquire):
// - HEAD.getAcquire(this): Ensures program order execution
//   visibility before reading state.
//
// CONSTRAINTS:
// - Direct access to volatile fields in hot-path is PROHIBITED.
```

### 1.2 Certificación de Rendimiento (`@AAACertified`)
Esta anotación documenta las garantías de latencia y throughput validadas.

**Formato Estándar**:

```java
import sv.dark.core.AAACertified;

/**
 * HIGH-PERFORMANCE CERTIFICATION
 *
 * RATIONALE:
 * - Document performance guarantees for static analysis.
 * - RetentionPolicy.SOURCE = 0ns execution overhead.
 *
 * TECHNICAL SPECS:
 * - maxLatencyNs: 1 (TSC Read)
 * - minThroughput: 60 (Fixed Layout)
 * - alignment: 64 (L1 Cache Line)
 */
@AAACertified(
    date = "2026-01-12",
    maxLatencyNs = 1,
    minThroughput = 60,
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "Temporal Determinism Unit - TSC-based"
)
public final class TimeKeeper { ... }
```

---

## 2. Estándares de Rendimiento (Hardware Targets)

### 2.1 Latencia Atómica
*   **Target**: < 150ns (Instrucción atomicidad hardware).
*   **Verificación**: Medición de ciclo exacto (`System.nanoTime()`).

### 2.2 Throughput de Memoria
*   **Target**: > 4.0 GB/s (Off-Heap Bandwidth).
*   **Implementación**: Uso estricto de `MemorySegment` (Zero-Copy).

### 2.3 Alineación de Caché
*   **Target**: 64 Bytes (L1 Cache Line).
*   **Validación**: Padding explícito para evitar False Sharing.

---

## 3. Patrones de Diseño de Bajo Nivel

### 3.1 Empaquetado Primitivo (Data Packing)
Uso de registros de 64 bits (`long`) para evitar direccionamiento indirecto.

```java
// [Float X: 32b] [Float Y: 32b]
public static long packVector2F(float x, float y) {
    long xBits = Float.floatToRawIntBits(x);
    long yBits = Float.floatToRawIntBits(y);
    return (xBits << 32) | (yBits & 0xFFFFFFFFL);
}
```

### 3.2 Gestión de VarHandles (Atomic Barriers)

| Operación | Instrucción Hardware (x86) | Semántica |
| :--- | :--- | :--- |
| `getAcquire()` | `MOV` (Ordered Load) | Barrera de Visibilidad (Visibility Barrier) |
| `setRelease()` | `MOV` (Ordered Store) | Barrera de Completitud (Completion Barrier) |
| `compareAndSet()` | `LOCK CMPXCHG` | Actualización Atómica |

### 3.3 Estructuras con Padding (Cache Isolation)

```java
// L1 ISOLATION: 64 Bytes total
long p1, p2, p3, p4, p5, p6, p7; // Prefix Padding (56B)
volatile long value;             // Payload (8B)
```

**Regla**: Cualquier contador volátil accedido por múltiples hilos requiere su propia línea de caché.

---

## 4. Anti-Patrones Prohibidos

### 4.1 Boxing/Unboxing en Hot-Paths
**PROHIBIDO**: Instanciación de wrappers (`Long`, `Integer`). Genera presión en GC y fallos de caché.
```java
// VIOLACIÓN CRÍTICA
queue.add(Long.valueOf(timestamp)); 
```

### 4.2 Bloqueo de Monitor (Synchronized)
**PROHIBIDO**: Uso de keywords `synchronized` o `locks` en rutas críticas de latencia.
*   **Alternativa**: `VarHandle` o algoritmos Wait-Free (Lock-Free).

### 4.3 I/O Síncrono
**PROHIBIDO**: `System.out.println` o I/O bloqueante dentro del loop de simulación.

---

## 5. Verificación de Cumplimiento

### 5.1 Suite de Validación
El código debe pasar las pruebas de integridad de memoria sin errores de alineación.

```bash
# Validación de Alineación y Padding
java sv.dark.bus.BusHardwareTest
```

### 5.2 Benchmarks
Validación continua de regresión de rendimiento.
*   Latencia `offer/poll` < 150ns.
*   Tasa de Fallos L1 < 0.1%.

---

**Estado**: VIGENTE
**Autoridad**: System Architect
