/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Validación de Baseline (Línea Base) para detección de Memory Leaks
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
package sv.volcan.kernel;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

public final class BaselineValidator {

    // ═══════════════════════════════════════════════════════════════════════════════
    // SYSTEM SNAPSHOT - Captura de Métricas del Sistema
    // ═══════════════════════════════════════════════════════════════════════════════

    public static class SystemSnapshot {
        public final long heapUsedBytes;
        public final long heapMaxBytes;
        public final long heapCommittedBytes;
        public final long nonHeapUsedBytes;
        public final int threadCount;
        public final long timestamp;
        public final String label;

        public SystemSnapshot(String label) {
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
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println("BASELINE SNAPSHOT: " + label);
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.printf("  Heap Used:      %,d bytes (%.2f MB)%n", heapUsedBytes, heapUsedBytes / 1_048_576.0);
            System.out.printf("  Heap Committed: %,d bytes (%.2f MB)%n", heapCommittedBytes,
                    heapCommittedBytes / 1_048_576.0);
            System.out.printf("  Heap Max:       %,d bytes (%.2f MB)%n", heapMaxBytes, heapMaxBytes / 1_048_576.0);
            System.out.printf("  Non-Heap Used:  %,d bytes (%.2f MB)%n", nonHeapUsedBytes,
                    nonHeapUsedBytes / 1_048_576.0);
            System.out.printf("  Thread Count:   %d%n", threadCount);
            System.out.printf("  Timestamp:      %,d ns%n", timestamp);
            System.out.println("═══════════════════════════════════════════════════════════════");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BASELINE COMPARISON - Comparación A vs C
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Compara dos snapshots y detecta memory leaks.
     * 
     * CRITERIOS DE VALIDACIÓN:
     * - Heap: C <= A + 1MB (tolerancia para Code Cache residual)
     * - Threads: C == A (no threads fantasma)
     * - Non-Heap: C <= A + 2MB (tolerancia para JIT compiler)
     * 
     * @param stateA Snapshot del Estado A (Sin Motor)
     * @param stateC Snapshot del Estado C (Post-Apagado)
     * @return true si no hay leaks, false si hay leaks
     */
    public static boolean validateCleanShutdown(SystemSnapshot stateA, SystemSnapshot stateC) {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("BASELINE VALIDATION: A (Sin Motor) vs C (Post-Apagado)");
        System.out.println("═══════════════════════════════════════════════════════════════");

        boolean passed = true;
        long TOLERANCE_1MB = 1_048_576; // 1 MB
        long TOLERANCE_2MB = 2_097_152; // 2 MB

        // Validación 1: Heap Memory
        long heapDelta = stateC.heapUsedBytes - stateA.heapUsedBytes;
        System.out.printf("  Heap Delta:     %,d bytes (%.2f MB)%n", heapDelta, heapDelta / 1_048_576.0);
        if (heapDelta > TOLERANCE_1MB) {
            System.out.println("  ❌ HEAP LEAK DETECTED: Delta > 1MB");
            passed = false;
        } else {
            System.out.println("  ✅ Heap OK: Delta <= 1MB");
        }

        // Validación 2: Non-Heap Memory (Project Panama, Code Cache)
        long nonHeapDelta = stateC.nonHeapUsedBytes - stateA.nonHeapUsedBytes;
        System.out.printf("  Non-Heap Delta: %,d bytes (%.2f MB)%n", nonHeapDelta, nonHeapDelta / 1_048_576.0);
        if (nonHeapDelta > TOLERANCE_2MB) {
            System.out.println("  ❌ NON-HEAP LEAK DETECTED: Delta > 2MB");
            passed = false;
        } else {
            System.out.println("  ✅ Non-Heap OK: Delta <= 2MB");
        }

        // Validación 3: Thread Count
        int threadDelta = stateC.threadCount - stateA.threadCount;
        System.out.printf("  Thread Delta:   %d%n", threadDelta);
        if (threadDelta > 0) {
            System.out.println("  ❌ THREAD LEAK DETECTED: Threads fantasma activos");
            passed = false;
        } else {
            System.out.println("  ✅ Threads OK: No hay threads fantasma");
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        if (passed) {
            System.out.println("✅ BASELINE VALIDATION PASSED: Shutdown limpio al 100%");
        } else {
            System.out.println("❌ BASELINE VALIDATION FAILED: Memory Leak detectado");
        }
        System.out.println("═══════════════════════════════════════════════════════════════\n");

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
        System.out.println("[BASELINE] Ejecutando ciclo triple de GC para limpieza profunda...");
        for (int i = 1; i <= 3; i++) {
            System.gc();
            try {
                Thread.sleep(500); // Drain period: 500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.printf("[BASELINE] GC Cycle %d/3 completado%n", i);
        }
        System.out.println("[BASELINE] Limpieza profunda completada");
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTOCOL A/B/C - Captura de Estados
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Captura el Estado A (Sin Motor).
     * 
     * @return Snapshot del sistema en reposo
     */
    public static SystemSnapshot captureStateA() {
        aggressiveGC(); // Limpiar basura residual antes de capturar
        SystemSnapshot snapshot = new SystemSnapshot("Estado A (Sin Motor)");
        snapshot.print();
        return snapshot;
    }

    /**
     * Captura el Estado B (Con Motor).
     * 
     * @return Snapshot del sistema durante ejecución
     */
    public static SystemSnapshot captureStateB() {
        SystemSnapshot snapshot = new SystemSnapshot("Estado B (Con Motor)");
        snapshot.print();
        return snapshot;
    }

    /**
     * Captura el Estado C (Post-Apagado).
     * 
     * IMPORTANTE: Ejecuta ciclo triple de GC antes de capturar.
     * 
     * @return Snapshot del sistema después del shutdown
     */
    public static SystemSnapshot captureStateC() {
        aggressiveGC(); // Forzar limpieza profunda antes de capturar
        SystemSnapshot snapshot = new SystemSnapshot("Estado C (Post-Apagado)");
        snapshot.print();
        return snapshot;
    }
}
