// Reading Order: 00010101
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import sv.dark.state.DarkStateVault;

/**
 * Zero-GC System Metrics Sampling.
 * 
 * <p>Captures dynamic system metrics (CPU Load, Free Memory) and
 * injects them into the StateVault as scaled integers to avoid
 * floating-point overhead.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 100_000, minThroughput = 60, alignment = 0, lockFree = true, offHeap = false, notes = "System Metrics Sampler (Scaled Integers)")
public final class DarkSystemProbe {

    private static final OperatingSystemMXBean OS_BEAN = (OperatingSystemMXBean) ManagementFactory
            .getOperatingSystemMXBean();

    // Vault Registers Addressing (Synchronous mapping)
    public static final int REG_CPU_LOAD = 500;
    public static final int REG_MEM_FREE = 501;
    public static final int REG_MEM_TOTAL = 502;

    private DarkSystemProbe() {
    } // Sealed: Sampling utility only.

    /**
     * Injects data as scaled integers to avoid floating-point overhead.
     * <p>[MECHANICAL SYMPATHY]: Representing 0-100% as 0-10000 to maintain 2
     * decimal points of precision.
     */
    public static void sample(DarkStateVault vault) {
        // CPU Load: (0.00% to 100.00%) -> Scaled integer
        int cpuScaled = (int) (OS_BEAN.getCpuLoad() * 10000);
        vault.write(REG_CPU_LOAD, cpuScaled);

        // Memory: Normalized to MB to avoid Integer overflows
        int freeMB = (int) (OS_BEAN.getFreeMemorySize() / 1048576);
        int totalMB = (int) (OS_BEAN.getTotalMemorySize() / 1048576);

        vault.write(REG_MEM_FREE, freeMB);
        vault.write(REG_MEM_TOTAL, totalMB);
    }

    /**
     * Static environment information.
     * Should only be invoked during the Kernel startup sequence.
     */
    public static void logStaticEnvironment() {
        DarkLogger.info("Boot", "OS: " + System.getProperty("os.name"));
        DarkLogger.info("Boot", "JVM: " + System.getProperty("java.version"));
    }
}
// updated 3/1/26
