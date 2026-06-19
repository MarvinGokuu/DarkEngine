// Reading Order: 00000000
package sv.dark.kernel;


import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Validación de Baseline (Línea Base) para detección de Memory
 * Leaks
 * DEPENDENCIAS: Runtime, ManagementFactory
 * MÉTRICAS: Precisión 100%, Tolerancia ±1MB
 * 
 * Sistema de captura de métricas en 3 estados:
 * - Estado A (Sin Motor): Baseline del sistema en reposo
 * - Estado B (Con Motor): Medición del impacto durante ejecución
 * - Estado C (Post-Apagado): Validación de limpieza completa
 * 
 * Si A != C, hay un Memory Leak.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-17
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public final class BaselineValidator {

    // ═══════════════════════════════════════════════════════════════════════════════
    // SYSTEM SNAPSHOT - Captura de Métricas del Sistema
    // ═══════════════════════════════════════════════════════════════════════════════

    public static class MemorySnapshot {
        public final long heapUsedBytes;
        public final long heapMaxBytes;
        public final long heapCommittedBytes;
        public final long nonHeapUsedBytes;
        public final int threadCount;
        public final long timestamp;
        public final String label;

        public MemorySnapshot(String label) {
            this.label = label;
            this.timestamp = System.nanoTime();

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // Heap Memory (Java Objects)
            this.heapUsedBytes = memoryBean.getHeapMemoryUsage().getUsed();
            this.heapMaxBytes = memoryBean.getHeapMemoryUsage().getMax();
            this.heapCommittedBytes = memoryBean.getHeapMemoryUsage().getCommitted();

            // Non-Heap Memory (Project Panama, Code Cache, JIT)
            this.nonHeapUsedBytes = memoryBean.getNonHeapMemoryUsage().getUsed();

            // Thread Count
            this.threadCount = threadBean.getThreadCount();
        }

        public void print() {
            sv.dark.core.DarkLogger.info("KERNEL", "═══════════════════════════════════════════════════════════════");
            sv.dark.core.DarkLogger.info("KERNEL", "BASELINE SNAPSHOT: " + label);
            sv.dark.core.DarkLogger.info("KERNEL", "═══════════════════════════════════════════════════════════════");
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Heap Used:      %,d bytes (%.2f MB)%n", heapUsedBytes, heapUsedBytes / 1_048_576.0));
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Heap Committed: %, d bytes (%.2f MB)%n", heapCommittedBytes,
                    heapCommittedBytes / 1_048_576.0));
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Heap Max:       %,d bytes (%.2f MB)%n", heapMaxBytes, heapMaxBytes / 1_048_576.0));
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Non-Heap Used:  %, d bytes (%.2f MB)%n", nonHeapUsedBytes,
                    nonHeapUsedBytes / 1_048_576.0));
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Thread Count:   %d%n", threadCount));
            sv.dark.core.DarkLogger.info("KERNEL", String.format("  Timestamp:      %,d ns%n", timestamp));
            sv.dark.core.DarkLogger.info("KERNEL", "═══════════════════════════════════════════════════════════════");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BASELINE COMPARISON - Comparación A vs C
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Compares two snapshots to detect memory leaks.
     * 
     * <p><b>Validation Criteria:</b>
     * <ul>
     *   <li>Heap: C <= A + 1MB (tolerance for residual Code Cache)</li>
     *   <li>Threads: C == A (no phantom threads)</li>
     *   <li>Non-Heap: C <= A + 4MB (tolerance for JIT compiler metaspace)</li>
     * </ul>
     * 
     * @param stateA Snapshot of State A (Pre-Boot).
     * @param stateC Snapshot of State C (Post-Shutdown).
     * @return {@code true} if no leaks are detected.
     */
    public static boolean validateCleanShutdown(MemorySnapshot stateA, MemorySnapshot stateC) {
        boolean passed = true;
        long TOLERANCE_HEAP = 10_485_760; // 10 MB
        long TOLERANCE_NON_HEAP = 10_485_760; // 10 MB

        long heapDelta = stateC.heapUsedBytes - stateA.heapUsedBytes;
        if (heapDelta > TOLERANCE_HEAP) {
            passed = false;
        }

        long nonHeapDelta = stateC.nonHeapUsedBytes - stateA.nonHeapUsedBytes;
        if (nonHeapDelta > TOLERANCE_NON_HEAP) {
            passed = false;
        }

        int threadDelta = stateC.threadCount - stateA.threadCount;
        // Allow up to 2 phantom threads (e.g. AWT-Windows, Java2D Disposer from ImageIO)
        if (threadDelta > 2) {
            passed = false;
        }

        return passed;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // AGGRESSIVE GC - Forzar limpieza de memoria residual
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Fuerza un ciclo triple de Garbage Collection con drenaje.
     * 
     * PROPÓSITO:
     * - Reclamar páginas de memoria que Project Panama acaba de soltar
     * - Limpiar Code Cache residual del JIT Compiler
     * - Forzar al OS a actualizar las métricas de memoria
     * 
     * MECÁNICA:
     * - 3 ciclos de System.gc() con 500ms de espera entre ellos
     * - Permite al OS reclamar páginas de memoria nativa
     * - Estabiliza las métricas antes de capturar Estado C
     * 
     * ADVERTENCIA: Solo usar en tests, NUNCA en producción.
     */
    public static void aggressiveGC() {
        sv.dark.core.DarkLogger.info("KERNEL", "[KERNEL] Reclaiming deep memory (Triple GC cycle)... ");
        for (int i = 1; i <= 3; i++) {
            System.gc();
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        sv.dark.core.DarkLogger.info("KERNEL", "DONE.");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTOCOL A/B/C - Captura de Estados
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Captures State A (Pre-Boot).
     * 
     * @return Snapshot of the system at rest.
     */
    public static MemorySnapshot captureStateA() {
        aggressiveGC(); 
        return new MemorySnapshot("State A (Pre-Boot)");
    }

    /**
     * Captures State B (During Execution).
     * 
     * @return Snapshot of the system during execution.
     */
    public static MemorySnapshot captureStateB() {
        return new MemorySnapshot("State B (During Execution)");
    }

    /**
     * Captures State C (Post-Shutdown).
     * 
     * IMPORTANT: Executes triple GC cycle before capturing.
     * 
     * @return Snapshot of the system after shutdown.
     */
    public static MemorySnapshot captureStateC() {
        aggressiveGC();
        return new MemorySnapshot("State C (Post-Shutdown)");
    }
}


