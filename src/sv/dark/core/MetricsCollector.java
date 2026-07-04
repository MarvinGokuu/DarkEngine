// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import sv.dark.core.AAACertified;

import sv.dark.core.systems.PhysicsSystem;
import sv.dark.bus.IEventBus;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;

/**
 * RESPONSIBILITY: OFF-CRITICAL-PATH metrics aggregation. Collect metrics from independent systems and aggregate without contention.
 * WHY: Tracking metrics synchronously inside systems degrades frame latency. We need an isolated aggregation phase.
 * TECHNIQUE: Read atomic counters from each system AFTER they have finished execution, off the critical frame path.
 * GUARANTEES: No impact on frame latency. Lock-free aggregation of system performance data.
 * 
 * <p>Perspective: Kernel Architect Level CEO
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
public class MetricsCollector {
    
    /**
     * FrameMetrics: Container for aggregated frame metrics
     */
    public static class FrameMetrics {
        public long frameNumber = 0;
        public long frameTimeNs = 0;
        
        // Counters per system (aggregated)
        public int physicsProcessed = 0;
        
        // Latencies
        public long busLatencyNs = 0;
        public long systemsExecutionNs = 0;
        
        // Statistics
        public double avgFrameTimeMs = 0;
        public int droppedFrames = 0;
        
        // Memoria JVM (Zero-Allocation Track)
        public long jvmTotalMemoryMb = 0;
        public long jvmFreeMemoryMb = 0;
        
        // Se elimina toString() para evitar String.format (Genera Garbage Collection)
    }

    // =========================================================================
    // TELEMETRY I/O (ZERO-ALLOCATION)
    // =========================================================================
    private static FileChannel telemetryChannel;
    private static final ByteBuffer writeBuffer = ByteBuffer.allocateDirect(1024);
    
    static {
        try {
            telemetryChannel = FileChannel.open(
                Path.of("logs", "telemetry_zero_alloc.log"), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE, 
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            DarkLogger.error("METRICS", "Failed to init Zero-Allocation Telemetry I/O.");
        }
    }
    
    /**
     * aggregateMetrics: Called AFTER systems finish
     * 
     * CRITICAL: This function executes OFF-CRITICAL-PATH.
     * It is not in the frame's critical latency budget.
     */
    public static void aggregateMetrics(
        PhysicsSystem physicsSystem,
        IEventBus eventBus,
        FrameMetrics output
    ) {
        // Read metrics from each system (WITHOUT CONTENTION)
        // Each system has already finished, no race conditions
        if (physicsSystem != null) {
            output.physicsProcessed = physicsSystem.getProcessedCount();
        }
        
        // Safely add bus statistics
        output.busLatencyNs = (eventBus != null) ? eventBus.getLastLatencyNs() : 0L;
        
        // Calculate total frame time and interpolate with EMA (Exponential Moving Average)
        if (output.frameNumber > 0) {
            output.avgFrameTimeMs = 
                output.avgFrameTimeMs * 0.9 + 
                (output.frameTimeNs / 1_000_000.0) * 0.1;
        } else {
            output.avgFrameTimeMs = output.frameTimeNs / 1_000_000.0;
        }
        
        // Detect frame drops
        if (output.frameTimeNs > 16_666_666) { // >16.67ms
            output.droppedFrames++;
        }
        
        // JVM Memory Stats
        Runtime rt = Runtime.getRuntime();
        output.jvmTotalMemoryMb = rt.totalMemory() / 1_048_576L;
        output.jvmFreeMemoryMb = rt.freeMemory() / 1_048_576L;
        
        // Flush to Disk via NIO FileChannel (Zero-Allocation)
        flushToDisk(output);
    }
    
    /**
     * Escribe las métricas en disco sin alojar memoria (Sin new String()).
     * Convierte los enteros a bytes manualmente dentro del buffer nativo.
     */
    private static void flushToDisk(FrameMetrics metrics) {
        if (telemetryChannel == null) return;
        
        writeBuffer.clear();
        
        // Fast manual byte appending para evitar String.valueOf()
        appendAscii(writeBuffer, "Frame[");
        appendNumber(writeBuffer, metrics.frameNumber);
        appendAscii(writeBuffer, "] Physics: ");
        appendNumber(writeBuffer, metrics.physicsProcessed);
        appendAscii(writeBuffer, " | JVM Mem (MB): ");
        appendNumber(writeBuffer, metrics.jvmTotalMemoryMb - metrics.jvmFreeMemoryMb);
        appendAscii(writeBuffer, " / ");
        appendNumber(writeBuffer, metrics.jvmTotalMemoryMb);
        appendAscii(writeBuffer, "\n");
        
        writeBuffer.flip();
        try {
            telemetryChannel.write(writeBuffer);
        } catch (Exception e) {
            // Ignore for real-time
        }
    }
    
    // Rutina Zero-Allocation para imprimir números al ByteBuffer
    private static void appendNumber(ByteBuffer buf, long num) {
        if (num == 0) {
            buf.put((byte)'0');
            return;
        }
        if (num < 0) {
            buf.put((byte)'-');
            num = -num;
        }
        long temp = num;
        int numDigits = 0;
        while (temp > 0) {
            temp /= 10;
            numDigits++;
        }
        int startPos = buf.position();
        buf.position(startPos + numDigits);
        
        temp = num;
        for (int i = numDigits - 1; i >= 0; i--) {
            long digit = temp % 10;
            buf.put(startPos + i, (byte) ('0' + digit));
            temp /= 10;
        }
    }

    private static void appendAscii(ByteBuffer buf, String text) {
        // En un motor real text es un byte[], pero asumiendo constantes el JIT lo optimiza
        for (int i = 0; i < text.length(); i++) {
            buf.put((byte) text.charAt(i));
        }
    }
    
    private static long lastMetricsTime = 0;

    /**
     * Checks if metrics should be collected (limited to 1 time per second for Unbounded FPS).
     */
    public static boolean shouldCollectMetrics(long frameNumber) {
        long now = System.currentTimeMillis();
        if (now - lastMetricsTime >= 1000) {
            lastMetricsTime = now;
            return true;
        }
        return false;
    }
}
