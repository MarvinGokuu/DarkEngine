package sv.volcan.net;

import sv.volcan.state.VolcanStateVault;
import sv.volcan.core.AAACertified;
import sv.volcan.state.VolcanStateLayout;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Physical memory metrics extraction from
 * SectorMemoryVault / StateVault.
 * GUARANTEES: Zero-allocation, non-blocking read (Atomic read), domain
 * isolation.
 * RESTRICTIONS: Forbidden to use java.awt, forbidden to format Strings,
 * forbidden to render.
 * CRITICAL DOMAIN: Telemetry
 *
 * @author Marvin-Dev
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 100, minThroughput = 100_000, alignment = 0, lockFree = true, offHeap = false, notes = "Memory Metrics Extractor (Zero-Allocation)")
public final class MemoryMonitor {

    // [HARD ENGINEERING]: Storage in primitive registers to avoid garbage
    // collector.
    private long lastFreeBytes;
    private long lastTotalBytes;

    /**
     * Captures instant snapshot of system memory registers.
     * Direct access to offsets without lookup overhead.
     */
    public void snapshot(VolcanStateVault context) {
        // Read directly from state buffer.
        // Assumes offsets are defined in VolcanStateLayout.
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