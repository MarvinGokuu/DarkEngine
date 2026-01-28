package sv.volcan.test;

import sv.volcan.kernel.BaselineValidator;
import sv.volcan.kernel.BaselineValidator.SystemSnapshot;
import sv.volcan.kernel.EngineKernel;
import sv.volcan.bus.VolcanEventDispatcher;
import sv.volcan.memory.SectorMemoryVault;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Test de Graceful Shutdown y Baseline Validation Protocol
 * DEPENDENCIAS: BaselineValidator, EngineKernel, VolcanEngineMaster
 * MÉTRICAS: Precisión 100%, Tolerancia ±1MB
 * 
 * Test del Protocolo A/B/C:
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
public class GracefulShutdownTest {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST: GRACEFUL SHUTDOWN & BASELINE VALIDATION PROTOCOL");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // ═══════════════════════════════════════════════════════════════
        // ESTADO A: SIN MOTOR (Baseline)
        // ═══════════════════════════════════════════════════════════════
        System.out.println(">>> CAPTURANDO ESTADO A (Sin Motor)...\n");
        SystemSnapshot stateA = BaselineValidator.captureStateA();

        // ═══════════════════════════════════════════════════════════════
        // ESTADO B: CON MOTOR (Ejecución)
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n>>> INICIANDO MOTOR (Estado B)...\n");

        // Crear y ejecutar el motor en un thread separado
        Thread engineThread = new Thread(() -> {
            try {
                // Crear kernel con dependencias
                VolcanEventDispatcher dispatcher = VolcanEventDispatcher.createDefault(14);
                SectorMemoryVault vault = new SectorMemoryVault(1024);
                EngineKernel kernel = new EngineKernel(dispatcher, vault);

                // Iniciar motor
                kernel.start();
            } catch (Exception e) {
                System.err.println("Error en motor: " + e.getMessage());
                e.printStackTrace();
            }
        }, "EngineThread");

        engineThread.start();

        // Esperar 5 segundos para que el motor se estabilice
        try {
            System.out.println(">>> Esperando 5 segundos para estabilización del motor...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Capturar Estado B (Con Motor)
        System.out.println("\n>>> CAPTURANDO ESTADO B (Con Motor)...\n");
        SystemSnapshot stateB = BaselineValidator.captureStateB();

        // Calcular impacto del motor
        long heapImpact = stateB.heapUsedBytes - stateA.heapUsedBytes;
        long nonHeapImpact = stateB.nonHeapUsedBytes - stateA.nonHeapUsedBytes;
        int threadImpact = stateB.threadCount - stateA.threadCount;

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("IMPACTO DEL MOTOR (B - A):");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.printf("  Heap Impact:      %,d bytes (%.2f MB)%n", heapImpact, heapImpact / 1_048_576.0);
        System.out.printf("  Non-Heap Impact:  %,d bytes (%.2f MB)%n", nonHeapImpact, nonHeapImpact / 1_048_576.0);
        System.out.printf("  Thread Impact:    %d threads%n", threadImpact);
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // ═══════════════════════════════════════════════════════════════
        // APAGADO SEGURO DEL MOTOR
        // ═══════════════════════════════════════════════════════════════
        System.out.println(">>> APAGANDO MOTOR (Graceful Shutdown)...\n");

        // Enviar señal de interrupción al thread del motor (cooperativa)
        engineThread.interrupt();

        // Esperar a que el motor termine (máximo 5 segundos)
        try {
            engineThread.join(5000);
            if (engineThread.isAlive()) {
                System.err.println("WARNING: Motor no terminó en 5 segundos");
                System.err.println("El Shutdown Hook debería ejecutarse al salir del programa");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n>>> Motor apagado\n");

        // ═══════════════════════════════════════════════════════════════
        // ESTADO C: POST-APAGADO (Validación de Limpieza)
        // ═══════════════════════════════════════════════════════════════
        System.out.println(">>> CAPTURANDO ESTADO C (Post-Apagado)...\n");
        SystemSnapshot stateC = BaselineValidator.captureStateC();

        // ═══════════════════════════════════════════════════════════════
        // VALIDACIÓN FINAL: A vs C
        // ═══════════════════════════════════════════════════════════════
        boolean passed = BaselineValidator.validateCleanShutdown(stateA, stateC);

        // ═══════════════════════════════════════════════════════════════
        // REPORTE FINAL
        // ═══════════════════════════════════════════════════════════════
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("REPORTE FINAL:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Estado A (Sin Motor):     " + formatSnapshot(stateA));
        System.out.println("Estado B (Con Motor):     " + formatSnapshot(stateB));
        System.out.println("Estado C (Post-Apagado):  " + formatSnapshot(stateC));
        System.out.println("═══════════════════════════════════════════════════════════════");

        if (passed) {
            System.out.println("✅ TEST PASSED: Graceful Shutdown exitoso - No hay memory leaks");
            System.exit(0);
        } else {
            System.out.println("❌ TEST FAILED: Memory Leak detectado - Revisar shutdown sequence");
            System.exit(1);
        }
    }

    private static String formatSnapshot(SystemSnapshot snapshot) {
        return String.format("Heap=%.2fMB, NonHeap=%.2fMB, Threads=%d",
                snapshot.heapUsedBytes / 1_048_576.0,
                snapshot.nonHeapUsedBytes / 1_048_576.0,
                snapshot.threadCount);
    }
}
