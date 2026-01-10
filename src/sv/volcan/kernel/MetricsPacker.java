/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Empaquetado de Métricas sin Allocations
 * DEPENDENCIAS: Ninguna (Bit manipulation pura)
 * MÉTRICAS: 0 allocations, <5ns latency
 * 
 * Empaqueta métricas del kernel en un long de 64 bits para transmisión
 * zero-copy a través del AdminBus.
 * 
 * LAYOUT (64 bits):
 * - Bits 0-15:  Frame count (16 bits, máx 65535)
 * - Bits 16-31: Total time en microsegundos (16 bits, máx 65ms)
 * - Bits 32-47: Reserved para eventos/segundo
 * - Bits 48-63: Metric type/flags
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-08
 */
package sv.volcan.kernel;

import sv.volcan.core.AAACertified;

@AAACertified(date = "2026-01-08", maxLatencyNs = 5, minThroughput = 1_000_000, alignment = 8, lockFree = true, offHeap = false, notes = "Metrics packing - 0 allocations, pure bit manipulation")
public final class MetricsPacker {

    private MetricsPacker() {
    } // Utility class

    // Metric Types
    public static final long TYPE_FRAME_STATS = 0x0001L << 48;
    public static final long TYPE_BUS_STATS = 0x0002L << 48;
    public static final long TYPE_WARNING = 0x0003L << 48;

    /**
     * Empaqueta estadísticas de frame en un long.
     * 
     * @param frameCount  Frame number (0-65535)
     * @param totalTimeNs Total frame time en nanosegundos
     * @return Packed metric
     */
    public static long packFrameStats(long frameCount, long totalTimeNs) {
        long frame = (frameCount & 0xFFFF);
        long timeUs = (totalTimeNs / 1000) & 0xFFFF; // Convert to microseconds
        return TYPE_FRAME_STATS | (timeUs << 16) | frame;
    }

    /**
     * Desempaqueta el frame count de una métrica.
     * 
     * @param packed Packed metric
     * @return Frame count
     */
    public static long unpackFrameCount(long packed) {
        return packed & 0xFFFF;
    }

    /**
     * Desempaqueta el tiempo total en microsegundos.
     * 
     * @param packed Packed metric
     * @return Time in microseconds
     */
    public static long unpackTimeMicros(long packed) {
        return (packed >> 16) & 0xFFFF;
    }

    /**
     * Retorna el tipo de métrica.
     * 
     * @param packed Packed metric
     * @return Metric type
     */
    public static long getMetricType(long packed) {
        return packed & (0xFFFFL << 48);
    }
}
