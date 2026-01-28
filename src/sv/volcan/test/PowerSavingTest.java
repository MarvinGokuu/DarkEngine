package sv.volcan.test;

import sv.volcan.kernel.EngineKernel;
import sv.volcan.bus.VolcanEventDispatcher;
import sv.volcan.memory.SectorMemoryVault;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Test de Escalado de Reposo (3 Niveles)
 * DEPENDENCIAS: EngineKernel, VolcanEventDispatcher, SectorMemoryVault
 * MÉTRICAS: Validación de consumo de CPU en idle
 * 
 * Test del Escalado de Reposo:
 * - Tier 1 (0-10s): Thread.onSpinWait() - Consumo medio
 * - Tier 2 (10s-1min): Thread.sleep(1) - Consumo bajo
 * - Tier 3 (>1min): Thread.sleep(100) - Consumo casi cero (10 despertares/seg)
 * 
 * Validación visual con Monitor de Recursos de Windows.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-17
 */
public class PowerSavingTest {

    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST: ESCALADO DE REPOSO (3 NIVELES)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Crear infraestructura del motor
        VolcanEventDispatcher dispatcher = VolcanEventDispatcher.createDefault(14);
        SectorMemoryVault vault = new SectorMemoryVault(1024);
        EngineKernel kernel = new EngineKernel(dispatcher, vault);

        // Instrucciones para el usuario
        System.out.println("Motor iniciado - Observa el Monitor de Recursos de Windows");
        System.out.println("\nPara abrir el Monitor de Recursos:");
        System.out.println("  1. Presiona Win+R");
        System.out.println("  2. Escribe: perfmon /res");
        System.out.println("  3. Ve a la pestaña 'CPU' y 'Memoria'\n");

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("ESCALADO ESPERADO:");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  Tier 0 (Activo):          CPU ~100% en 1 core");
        System.out.println("  Tier 1 (Spin Wait):       CPU ~50-70% después de 10s idle");
        System.out.println("  Tier 2 (Light Sleep):     CPU ~5-10% después de 20s idle");
        System.out.println("  Tier 3 (Deep Hibernation): CPU ~0-1% después de 1min idle");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("⏱️  El motor NO tiene eventos, así que entrará en modo idle automáticamente.");
        System.out.println("📊 Observa cómo el consumo de CPU baja progresivamente en el Monitor.\n");

        System.out.println("Presiona Ctrl+C para terminar (ejecutará Graceful Shutdown)\n");

        // Iniciar motor
        kernel.start();
    }
}
