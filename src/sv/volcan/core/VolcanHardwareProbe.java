package sv.volcan.core; // Sincronizado con la ruta física src/sv/volcan/core/

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Telemetría de Hardware y Detección de Capacidades.
 * DEPENDENCIAS: java.lang.management.OperatingSystemMXBean
 * MÉTRICAS: Zero-Latency Access (Cached)
 * 
 * Sonda de hardware estática. Captura las capacidades físicas del sistema
 * (RAM, Cores, Industrial Grade) durante el arranque para evitar syscalls en
 * runtime.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanHardwareProbe {

    // Cache de Hardware (Inmutable tras la fase de Ignición)
    private static final long PHYSICAL_MEMORY;
    private static final int LOGICAL_CORES;
    private static final boolean IS_INDUSTRIAL_GRADE;

    static {
        // Captura única de la realidad física del silicio
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        PHYSICAL_MEMORY = osBean.getTotalMemorySize();
        LOGICAL_CORES = Runtime.getRuntime().availableProcessors();

        // Umbral de grado industrial: 8GB de RAM física mínima detectada
        IS_INDUSTRIAL_GRADE = PHYSICAL_MEMORY >= (8L * 1024 * 1024 * 1024);
    }

    private VolcanHardwareProbe() {
    } // Sellado: Solo acceso a telemetría estática.

    /**
     * Retorna la memoria física en bytes sin invocar al SO de nuevo (O(1)).
     */
    public static long getPhysicalMemory() {
        return PHYSICAL_MEMORY;
    }

    /**
     * Retorna el número de núcleos para el MultiCoreScheduler.
     */
    public static int getCoreCount() {
        return LOGICAL_CORES;
    }

    public static boolean isIndustrial() {
        return IS_INDUSTRIAL_GRADE;
    }

    /**
     * Diagnóstico de arranque (Solo fase de Ignición).
     * [NOTA TÉCNICA]: Se permite String.format únicamente en esta fase de
     * pre-arranque.
     */
    public static void emitReport() {
        String report = String.format(
                "[PROBE] Hardware Verified: %d GB RAM | %d Cores | Profile: %s",
                PHYSICAL_MEMORY / (1024 * 1024 * 1024),
                LOGICAL_CORES,
                IS_INDUSTRIAL_GRADE ? "INDUSTRIAL" : "RESTRICTED");
        System.out.println(report);
    }
}
// actualizado3/1/26