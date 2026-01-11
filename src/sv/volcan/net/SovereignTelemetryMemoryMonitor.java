package sv.volcan.net;

import sv.volcan.state.VolcanStateVault;
import sv.volcan.core.AAACertified;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Extracción de métricas de memoria física del
 * SectorMemoryVault / StateVault.
 * GARANTÍAS: Zero-allocation, lectura no bloqueante (Atomic read), aislamiento
 * de dominio.
 * PROHIBICIONES: Prohibido usar java.awt, prohibido formatear Strings,
 * prohibido renderizar.
 * DOMINIO CRÍTICO: Telemetry
 *
 * @author Marvin-Dev
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 100, minThroughput = 100_000, alignment = 0, lockFree = true, offHeap = false, notes = "Memory Metrics Extractor (Zero-Allocation)")
public final class SovereignTelemetryMemoryMonitor {

    // [INGENIERÍA DURA]: Almacenamiento en registros primitivos para evitar el
    // recolector de basura.
    private long lastFreeBytes;
    private long lastTotalBytes;

    /**
     * Captura una imagen instantánea de los registros de memoria del sistema.
     * Acceso directo a offsets sin overhead de búsqueda.
     */
    public void snapshot(VolcanStateVault context) {
        // Leemos directamente del buffer de estado soberano.
        // Se asume que los offsets están definidos en VolcanStateLayout.
        this.lastFreeBytes = context.readLong(VolcanStateLayout.METRIC_RAM_FREE);
        this.lastTotalBytes = context.readLong(VolcanStateLayout.METRIC_RAM_TOTAL);
    }

    public long getFreeBytes() {
        return lastFreeBytes;
    }

    public long getTotalBytes() {
        return lastTotalBytes;
    }

    /**
     * Calcula la utilización porcentual mediante aritmética entera escalada.
     * 
     * @return Uso en base 10000 (ej. 8500 = 85.00%)
     */
    public int getUsageBasisPoints() {
        if (lastTotalBytes == 0)
            return 0;
        return (int) (((lastTotalBytes - lastFreeBytes) * 10000) / lastTotalBytes);
    }
}
// actualizado3/1/26