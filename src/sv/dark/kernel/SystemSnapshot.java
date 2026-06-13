// Reading Order: 00010010
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Immutable snapshot structure capturing the hardware and OS-level execution state.
 * WHY: Post-mortem analysis and OS state cleanup require exact telemetry of the machine state prior to running the engine.
 * TECHNIQUE: Captures Thread Affinity, Power Scheme GUIDs, and Power Source strings in a final, immutable class.
 * GUARANTEES: Thread-confined heap properties. Snapshot variables are final and immutable. Deterministic tracing.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-08",
    maxLatencyNs = 100,
    minThroughput = 0,
    alignment    = 0,
    lockFree     = true,
    offHeap      = false,
    notes        = "Immutable telemetry record for execution state"
)
public final class SystemSnapshot {

    public final long threadAffinityMask;
    public final String powerSchemeGuid;
    public final String powerSchemeName;
    public final String powerSource;
    public final long timestamp;

    public SystemSnapshot(long threadAffinityMask, String powerSchemeGuid, String powerSchemeName, String powerSource) {
        this.threadAffinityMask = threadAffinityMask;
        this.powerSchemeGuid = powerSchemeGuid;
        this.powerSchemeName = powerSchemeName;
        this.powerSource = powerSource;
        this.timestamp = System.nanoTime();
    }

    /**
     * Formats the telemetry data without blocking I/O.
     * @return Formatted snapshot string.
     */
    public String formatTelemetryData() {
        return String.format("SNAPSHOT | ThreadMask: 0x%X | PowerGuid: %s | PowerName: %s | PowerSrc: %s | Timestamp: %,d ns",
                threadAffinityMask, powerSchemeGuid, powerSchemeName, powerSource, timestamp);
    }
}
