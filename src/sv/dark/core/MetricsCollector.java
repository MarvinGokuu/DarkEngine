// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import sv.dark.core.AAACertified;

import sv.dark.core.systems.MovementSystem;
import sv.dark.core.systems.RenderSystem;
import sv.dark.core.systems.PhysicsSystem;
import sv.dark.core.systems.AudioSystem;
import sv.dark.bus.IEventBus;

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
@AAACertified(date = "2026-06-11", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = false, offHeap = false, notes = "Automatically AAA Certified during Core Audit")
public class MetricsCollector {
    
    /**
     * FrameMetrics: Container for aggregated frame metrics
     */
    public static class FrameMetrics {
        public long frameNumber = 0;
        public long frameTimeNs = 0;
        
        // Counters per system (aggregated)
        public int movementProcessed = 0;
        public int renderProcessed = 0;
        public int physicsProcessed = 0;
        public int audioProcessed = 0;
        
        // Latencies
        public long busLatencyNs = 0;
        public long systemsExecutionNs = 0;
        
        // Statistics
        public double avgFrameTimeMs = 0;
        public int droppedFrames = 0;
        
        @Override
        public String toString() {
            return String.format(
                "Frame[%d] Time: %.2f ms | Latency: %d ns | " +
                "Movement: %d | Render: %d | Physics: %d | Audio: %d",
                frameNumber,
                frameTimeNs / 1_000_000.0,
                busLatencyNs,
                movementProcessed,
                renderProcessed,
                physicsProcessed,
                audioProcessed
            );
        }
    }
    
    /**
     * aggregateMetrics: Called AFTER systems finish
     * 
     * CRITICAL: This function executes OFF-CRITICAL-PATH.
     * It is not in the frame's critical latency budget.
     */
    public static void aggregateMetrics(
        MovementSystem movementSystem,
        RenderSystem renderSystem,
        PhysicsSystem physicsSystem,
        AudioSystem audioSystem,
        IEventBus eventBus,
        FrameMetrics output
    ) {
        // Read metrics from each system (WITHOUT CONTENTION)
        // Each system has already finished, no race conditions
        if (movementSystem != null) {
            output.movementProcessed = movementSystem.getProcessedCount();
        }
        if (renderSystem != null) {
            output.renderProcessed = renderSystem.getProcessedCount();
        }
        if (physicsSystem != null) {
            output.physicsProcessed = physicsSystem.getProcessedCount();
        }
        if (audioSystem != null) {
            output.audioProcessed = audioSystem.getProcessedCount();
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
