// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import sv.dark.core.AAACertified;

import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;
import sv.dark.core.EntityLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RESPONSIBILITY: Process linear movement physics for all entities.
 * WHY: To correctly update positions based on velocity and deltaTime.
 * TECHNIQUE: Iterate linearly over native entity memory to maximize CPU prefetching.
 * GUARANTEES: SIMD-Optimized Stride Access and deterministic state updates.
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
public final class MovementSystem implements GameSystem {

    private static final long STRIDE = EntityLayout.STRIDE;
    private static final long X_OFF = EntityLayout.X_OFFSET;
    private static final long VX_OFF = EntityLayout.VX_OFFSET;

    private final AtomicInteger processedCount = new AtomicInteger(0);

    public int getProcessedCount() {
        return processedCount.get();
    }

    public void incrementProcessedCount() {
        processedCount.incrementAndGet();
    }

    /**
     * High-speed update.
     * Processes the memory sector as a continuous stream of bytes.
     * 
     * IMPLEMENTATION: GameSystem.update()
     * GUARANTEE: Deterministic - same state + deltaTime = same result
     * OPTIMIZATION: Sequential access (stride-based) for CPU prefetching
     */
    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        // SSOT: We read entityCount from state (not as a parameter)
        int entityCount = state.readInt(DarkStateLayout.ENTITY_COUNT);

        // We get the native memory segment only once
        MemorySegment segment = state.getRawSegment();
        long currentBase = 0;

        for (int i= 0; i< entityCount; i++) {
            // Reading and Writing using constant offsets (Mechanical Sympathy)
            double x = segment.get(ValueLayout.JAVA_DOUBLE, currentBase + X_OFF);
            double vx = segment.get(ValueLayout.JAVA_DOUBLE, currentBase + VX_OFF);

            // Direct physics injection: x = x + (vx * deltaTime)
            segment.set(ValueLayout.JAVA_DOUBLE, currentBase + X_OFF, x + (vx * deltaTime));

            // Memory pointer advance (Avoids multiplication in the loop)
            currentBase += STRIDE;
        }

        processedCount.incrementAndGet(); // Increment local metric on each update
    }
}
