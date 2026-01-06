package sv.volcan.core;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import sv.volcan.state.VolcanStateVault;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Inyección de métricas de hardware sin asignación de memoria
 * (Zero-GC).
 * GARANTÍAS: Zero-GC, acceso por registro binario, métricas escaladas para
 * precisión industrial.
 * PROHIBICIONES: Prohibido usar Double o Float para persistencia en Vault;
 * prohibido loggear en el sampleo.
 * DOMINIO CRÍTICO: Telemetría / Diagnóstico
 */
public final class VolcanSystemProbe {

    private static final OperatingSystemMXBean OS_BEAN = (OperatingSystemMXBean) ManagementFactory
            .getOperatingSystemMXBean();

    // Direccionamiento de Registros en el Vault (Mapeo síncrono)
    public static final int REG_CPU_LOAD = 500;
    public static final int REG_MEM_FREE = 501;
    public static final int REG_MEM_TOTAL = 502;

    private VolcanSystemProbe() {
    } // Sellado: Solo utilidad de muestreo.

    /**
     * Inyecta datos como enteros escalados para evitar el overhead de punto
     * flotante.
     * [MECHANICAL SYMPATHY]: Representación de 0-100% como 0-10000 para mantener 2
     * decimales de precisión.
     */
    public static void sample(VolcanStateVault vault) {
        // CPU Load: (0.00% a 100.00%) -> Entero escalado
        int cpuScaled = (int) (OS_BEAN.getCpuLoad() * 10000);
        vault.write(REG_CPU_LOAD, cpuScaled);

        // Memoria: Normalizada a MB para evitar desbordamientos de Integer
        int freeMB = (int) (OS_BEAN.getFreeMemorySize() / 1048576);
        int totalMB = (int) (OS_BEAN.getTotalMemorySize() / 1048576);

        vault.write(REG_MEM_FREE, freeMB);
        vault.write(REG_MEM_TOTAL, totalMB);
    }

    /**
     * Información estática del entorno.
     * Solo debe invocarse durante la secuencia de ignición del Kernel.
     */
    public static void logStaticEnvironment() {
        System.out.println("[VOLCAN-BOOT] OS: " + System.getProperty("os.name"));
        System.out.println("[VOLCAN-BOOT] JVM: " + System.getProperty("java.version"));
    }
}
// actualizado3/1/26
