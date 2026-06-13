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
 *   <li>Bits 0-15: Frame count (16 bits, max 65535)</li>
 *   <li>Bits 16-31: Total time in microseconds (16 bits, max 65ms)</li>
 *   <li>Bits 32-47: Reserved for events/second</li>
 *   <li>Bits 48-63: Metric type/flags</li>
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

    // Metric Types
    public static final long TYPE_FRAME_STATS = 0x0001L << 48;
    public static final long TYPE_BUS_STATS = 0x0002L << 48;
    public static final long TYPE_WARNING = 0x0003L << 48;

    /**
     * Packs frame statistics into a single 64-bit long.
     * 
     * @param frameCount  Frame number (0-65535).
     * @param totalTimeNs Total frame time in nanoseconds.
     * @return Packed metric.
     */
    public static long packFrameStats(long frameCount, long totalTimeNs) {
        long frame = (frameCount & 0xFFFFL);
        // Mechanical Sympathy: Division by 1000 converted to multiply + bitshift.
        // 2^32 / 1000 = 4294967.296. So (X * 4294967) >> 32 is equivalent to X / 1000.
        long timeUs = ((totalTimeNs * 4294967L) >> 32) & 0xFFFFL;
        return TYPE_FRAME_STATS | (timeUs << 16) | frame;
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

    /**
     * Unpacks the total time in microseconds from a packed metric.
     * 
     * @param packed Packed metric.
     * @return Time in microseconds.
     */
    public static long unpackTimeMicros(long packed) {
        return (packed >> 16) & 0xFFFFL;
    }

    /**
     * Returns the metric type from a packed metric.
     * 
     * @param packed Packed metric.
     * @return Metric type.
     */
    public static long getMetricType(long packed) {
        return packed & (0xFFFFL << 48);
    }
}
