// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core; // Sincronizado con la ruta fisica src/sv/dark/core/

import sv.dark.core.AAACertified;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * Hardware Telemetry and Capability Detection.
 * 
 * <p>Static hardware probe. Captures physical system capabilities
 * (RAM, Cores, Industrial Grade) during boot to avoid runtime syscalls.
 * 
 * <p>Metrics: Zero-Latency Access (Cached)
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public final class DarkHardwareProbe {

    // Hardware Cache (Immutable after Startup phase)
    private static final long PHYSICAL_MEMORY;
    private static final int LOGICAL_CORES;
    private static final boolean IS_INDUSTRIAL_GRADE;

    static {
        // Single capture of physical hardware reality
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        PHYSICAL_MEMORY = osBean.getTotalMemorySize();
        LOGICAL_CORES = Runtime.getRuntime().availableProcessors();

        // Industrial grade threshold: Minimum 8GB of physical RAM detected
        IS_INDUSTRIAL_GRADE = PHYSICAL_MEMORY >= (8L * 1024 * 1024 * 1024);
    }

    private DarkHardwareProbe() {
    } // Sealed: Only static telemetry access.

    /**
     * Returns physical memory in bytes without invoking the OS again (O(1)).
     */
    public static long getPhysicalMemory() {
        return PHYSICAL_MEMORY;
    }

    /**
     * Returns the number of cores for the MultiCoreScheduler.
     */
    public static int getCoreCount() {
        return LOGICAL_CORES;
    }

    public static boolean isIndustrial() {
        return IS_INDUSTRIAL_GRADE;
    }

    /**
     * Boot diagnostic (Startup phase only).
     * [TECHNICAL NOTE]: String.format is allowed only in this
     * pre-boot phase.
     */
    public static void emitReport() {
        String report = String.format(
                "Hardware Verified: %d GB RAM | %d Cores | Profile: %s",
                PHYSICAL_MEMORY / (1024 * 1024 * 1024),
                LOGICAL_CORES,
                IS_INDUSTRIAL_GRADE ? "INDUSTRIAL" : "RESTRICTED");
        DarkLogger.info("Probe", report);
    }
}
// updated 3/1/26
