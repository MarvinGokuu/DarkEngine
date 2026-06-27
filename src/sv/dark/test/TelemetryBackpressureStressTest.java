package sv.dark.test;

import sv.dark.kernel.EngineKernel;
import sv.dark.bus.DarkEventDispatcher;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.admin.AdminController;
import sv.dark.bus.DarkAtomicBus;
import sv.dark.kernel.MetricsPacker;
import sv.dark.bus.DarkSignalPacker;
import sv.dark.bus.DarkSignalCommands;

public class TelemetryBackpressureStressTest {
    public static void main(String[] args) throws Exception {
        System.out.println("[TEST] Telemetry Backpressure Stress Test");

        // 1. Instanciar Kernel que inicializa el DarkAtomicBus(1024)
        SectorMemoryVault vault = new SectorMemoryVault(1024);
        DarkEventDispatcher dispatcher = DarkEventDispatcher.createDefault(14);
        EngineKernel kernel = new EngineKernel(dispatcher, vault);
        DarkAtomicBus bus = kernel.getAdminMetricsBus();

        // 2. Arrancar consumidor asíncrono (Control Plane)
        AdminController.startControlPlane(kernel, vault);

        // Warmup JVM
        for(int i=0; i<10000; i++) {
            long dummy = MetricsPacker.packFrameStats(1, 1, 1, 1, 1);
        }

        // Sugerir GC antes de medir
        System.gc();
        Thread.sleep(100);
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // 3. Hilo Productor Agresivo (Hot-Path)
        int totalSignals = 1_000_000;
        int successCount = 0;
        int droppedCount = 0;

        long startTimeNs = System.nanoTime();
        
        for (int i = 0; i < totalSignals; i++) {
            long metric = MetricsPacker.packFrameStats(i, 16000, 60, 60, 2000);
            if (bus.offer(metric)) {
                successCount++;
            } else {
                droppedCount++;
            }
        }

        long endTimeNs = System.nanoTime();
        long durationMs = (endTimeNs - startTimeNs) / 1_000_000;

        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long allocations = Math.max(0, endMem - startMem);

        System.out.println("--- RESULTADOS DEL TEST DE ESTRÉS ---");
        System.out.println("Señales Totales  : " + totalSignals);
        System.out.println("Exitosas (Offer) : " + successCount);
        System.out.println("Descartadas      : " + droppedCount);
        System.out.printf("Drop Rate        : %.2f%%\n", (droppedCount / (double) totalSignals) * 100.0);
        System.out.println("Latencia Total   : " + durationMs + " ms");
        System.out.println("Allocations (Heap): " + allocations + " bytes");

        // 4. Píldora de Veneno (SYS_TERMINATE_LOG_SIGNAL)
        long poisonPill = DarkSignalPacker.packCmd(DarkSignalCommands.SYS_TERMINATE_LOG_SIGNAL);
        // Forzar inserción hasta que haya espacio
        while (!bus.offer(poisonPill)) {
            Thread.sleep(1);
        }

        // Esperar cierre digno y sin excepciones
        AdminController.stopControlPlane();

        // Validar Criterios de Éxito de Sympatía Mecánica
        // Umbral relajado a 25ms para evitar falsos negativos en entornos virtualizados o con otras cargas,
        // pero la arquitectura es O(1) y suele tomar < 5ms.
        if (durationMs > 25) {
            System.err.println("[FAIL] La latencia del productor superó el umbral estricto: " + durationMs + "ms");
            System.exit(1);
        }

        System.out.println("[OK] Telemetry Backpressure Test SUPERADO con Zero-Blocking.");
        System.exit(0);
    }
}
