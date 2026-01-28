package sv.volcan.test;

import sv.volcan.kernel.TimeKeeper;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Test de Validación del Dynamic Performance Governor
 * DEPENDENCIAS: TimeKeeper
 * MÉTRICAS: Validación de cambio de marchas (60 -> 120 -> 144 -> 60)
 * 
 * Simula cargas de trabajo variables para forzar al Governor a:
 * 1. Subir de marcha en situaciones ligeras (Menús, exploración).
 * 2. Bajar de marcha ante picos de estrés (Combate intenso, explosiones).
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-20
 */
public class GovernorTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST: VOLCAN GOVERNOR (DYNAMIC FPS SCALING)");
        System.out.println("═══════════════════════════════════════════════════════════════");

        TimeKeeper timeKeeper = new TimeKeeper();

        System.out.println("\n[PHASE 1] Warmup (Simulando carga ligera)...");
        // Debería subir a Gear 2 (120 FPS) y luego Gear 3 (144 FPS) si es
        // suficientemente rápido
        simulateFrames(timeKeeper, 180, 1_000_000); // 1ms de carga (muy ligero)

        System.out.println("\n[PHASE 2] Stress Test (Simulando 'Cyberpunk' Load)...");
        // Simulamos un frame pesado de 12ms.
        // A 144 FPS (7ms budget) esto es inaceptable -> Debería bajar a Gear 1 (60 FPS,
        // 16ms budget)
        simulateFrames(timeKeeper, 60, 12_000_000); // 12ms de carga

        System.out.println("\n[PHASE 3] Recovery (Volviendo a la calma)...");
        // Debería recuperar marchas poco a poco
        simulateFrames(timeKeeper, 200, 2_000_000); // 2ms de carga

        System.out.println("\n[PHASE 4] TNT OVERLOAD (Simulando explosión masiva en Minecraft)...");
        // 50ms de carga por frame. Esto rompe incluso el presupuesto de 60 FPS (16ms).
        // El sistema debe mantenerse en Gear 1 y reportar warnings, sin colapsar.
        simulateFrames(timeKeeper, 30, 50_000_000); // 50ms de carga

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("TEST COMPLETE");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    private static void simulateFrames(TimeKeeper tk, int frames, long workloadNs) {
        for (int i = 0; i < frames; i++) {
            tk.startFrame();

            // Simular trabajo (Sleeper)
            long startWork = System.nanoTime();
            while (System.nanoTime() - startWork < workloadNs) {
                Thread.onSpinWait();
            }

            // Registrar tiempo simluado en Phase 3 (Systems)
            tk.recordPhaseTime(3, workloadNs);

            // Imprimir estado cada 30 frames para no saturar consola
            if (i % 30 == 0) {
                tk.printStats();
            }

            tk.waitForNextFrame();
        }
    }
}
