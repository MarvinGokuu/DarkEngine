// Reading Order: 00010001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: High-performance 64-bit atomic metrics packer.
 * WHY: Transferring multiple metrics across threads requires object instantiation (GC overhead). Packing them into a single primitive eliminates allocations.
 * TECHNIQUE: Packages kernel metrics into a 64-bit long for zero-copy transmission across the AdminBus using purely bitwise manipulation.
 * GUARANTEES: Zero allocations and wait-free execution.
 *
 * <p>LAYOUT (64 bits):
 * <ul>
 *   <li>Bits 0-15: Frame count (16 bits)</li>
 *   <li>Bits 16-31: Total time in microseconds (16 bits)</li>
 *   <li>Bits 32-39: Target FPS (8 bits)</li>
 *   <li>Bits 40-47: Actual FPS (8 bits)</li>
 *   <li>Bits 48-59: Headroom in 10-us units (12 bits, signed)</li>
 *   <li>Bits 60-63: Metric type (4 bits)</li>
 * </ul>
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-08",
    maxLatencyNs = 5,
    minThroughput = 1_000_000,
    alignment    = 8,
    lockFree     = true,
    offHeap      = false,
    notes        = "Metrics packing - 0 allocations, pure bit manipulation"
)
public final class MetricsPacker {

    private MetricsPacker() {
        throw new AssertionError("MetricsPacker is a static utility class");
    }

    // Metric Types (Bits 60-63)
    public static final long TYPE_FRAME_STATS = 0x1L << 60;
    public static final long TYPE_BUS_STATS = 0x2L << 60;
    public static final long TYPE_WARNING = 0x3L << 60;

    /**
     * Packs frame statistics into a single 64-bit long.
     */
    public static long packFrameStats(long frameCount, long totalTimeNs, long targetFps, long actualFps, long headroomNs) {
        long frame = (frameCount & 0xFFFFL);
        long timeUs = ((totalTimeNs * 4294967L) >> 32) & 0xFFFFL;
        long target = (targetFps & 0xFFL);
        long actual = (actualFps & 0xFFL);
        long head10us = (headroomNs / 10000L) & 0xFFFL; // 12 bits signed
        
        return TYPE_FRAME_STATS | (head10us << 48) | (actual << 40) | (target << 32) | (timeUs << 16) | frame;
    }

    /**
     * Unpacks the frame count from a packed metric.
     * 
     * @param packed Packed metric.
     * @return Frame count.
     */
    public static long unpackFrameCount(long packed) {
        return packed & 0xFFFFL;
    }

    public static long unpackTimeMicros(long packed) {
        return (packed >> 16) & 0xFFFFL;
    }

    public static long unpackTargetFps(long packed) {
        return (packed >> 32) & 0xFFL;
    }

    public static long unpackActualFps(long packed) {
        return (packed >> 40) & 0xFFL;
    }

    public static long unpackHeadroomNs(long packed) {
        long head10us = (packed >> 48) & 0xFFFL;
        // Sign extend 12 bits to 64 bits
        if ((head10us & 0x800L) != 0) {
            head10us |= 0xFFFFFFFFFFFFF000L;
        }
        return head10us * 10000L;
    }

    public static long getMetricType(long packed) {
        return packed & (0xFL << 60);
    }
}
