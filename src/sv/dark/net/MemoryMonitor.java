// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.net;

import sv.dark.state.DarkStateVault;
import sv.dark.core.AAACertified;
import sv.dark.state.DarkStateLayout;

/**
 * Physical Memory Metrics Extractor.
 * 
 * <p>Extracts metrics from the SectorMemoryVault / StateVault.
 * 
 * <p>GUARANTEES:
 * <ul>
 *   <li>Zero-allocation</li>
 *   <li>Non-blocking read (Atomic read)</li>
 *   <li>Domain isolation</li>
 * </ul>
 * 
 * <p>RESTRICTIONS:
 * <ul>
 *   <li>Forbidden to use java.awt</li>
 *   <li>Forbidden to format Strings</li>
 *   <li>Forbidden to render</li>
 * </ul>
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 100, minThroughput = 100_000, alignment = 0, lockFree = true, offHeap = false, notes = "Memory Metrics Extractor (Zero-Allocation)")
public final class MemoryMonitor {

    // [HARD ENGINEERING]: Storage in primitive registers to avoid garbage collector.
    private long lastFreeBytes;
    private long lastTotalBytes;

    /**
     * Captures instant snapshot of system memory registers.
     * Direct access to offsets without lookup overhead.
     */
    public void snapshot(DarkStateVault context) {
        // Read directly from state buffer.
        // Assumes offsets are defined in DarkStateLayout.
        this.lastFreeBytes = context.readLong(DarkStateLayout.METRIC_RAM_FREE);
        this.lastTotalBytes = context.readLong(DarkStateLayout.METRIC_RAM_TOTAL);
    }

    public long getFreeBytes() {
        return lastFreeBytes;
    }

    public long getTotalBytes() {
        return lastTotalBytes;
    }

    /**
     * Calculates percentage utilization using scaled integer arithmetic.
     * 
     * @return Usage in basis points (e.g. 8500 = 85.00%).
     */
    public int getUsageBasisPoints() {
        if (lastTotalBytes == 0)
            return 0;
        return (int) (((lastTotalBytes - lastFreeBytes) * 10000) / lastTotalBytes);
    }
}
// updated 3/1/26
