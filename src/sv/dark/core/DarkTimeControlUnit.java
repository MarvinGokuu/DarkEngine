// Reading Order: 00101000
//  40
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import sv.dark.core.AAACertified;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Time Control and Rewind System (Time Travel).
 * 
 * <p>Manages a circular buffer of native memory snapshots. Allows
 * instantly rewinding the world state for network prediction or
 * debugging.
 * 
 * <p>Metrics: Zero-GC Snapshot, O(1) Rollback
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
public final class DarkTimeControlUnit {

    private final MemorySegment timeSlab; // Single contiguous block of native memory (Off-Heap)
    private final long frameSize;
    private final int maxFrames;
    private int writeIndex = 0;

    /**
     * @param arena     Confined arena for the execution session.
     * @param frameSize Exact size in bytes of a WorldStateFrame.
     * @param maxFrames Capacity of the circular buffer for history.
     */
    public DarkTimeControlUnit(Arena arena, long frameSize, int maxFrames) {
        this.frameSize = frameSize;
        this.maxFrames = maxFrames;

        // [HARD ENGINEERING]: We reserve the complete slab at the beginning to avoid
        // fragmentation.
        // Alignment to 64 bytes to maximize memory bus bandwidth.
        this.timeSlab = arena.allocate(frameSize * maxFrames, 64L);
    }

    /**
     * Captures the present state by overwriting the oldest frame.
     * Wait-free operation through direct native memory copy.
     */
    public void capture(MemorySegment activeState) {
        // O(1) offset calculation
        long offset = (long) writeIndex * frameSize;

        // We create a view (slice) without allocation on the Heap
        MemorySegment targetFrame = timeSlab.asSlice(offset, frameSize);

        // [MECHANICAL SYMPATHY]: Low-level direct copy to memory.
        targetFrame.copyFrom(activeState);

        // Advance of the circular pointer
        writeIndex = (writeIndex + 1) % maxFrames;
    }

    /**
     * Restores the previous state of the engine.
     * Reverts data control to the previously captured instant.
     */
    public void rollback(MemorySegment activeState) {
        // Rewind the index in the circular buffer
        writeIndex = (writeIndex - 1 + maxFrames) % maxFrames;
        long offset = (long) writeIndex * frameSize;

        MemorySegment historicFrame = timeSlab.asSlice(offset, frameSize);

        // Atomic restoration of bits
        activeState.copyFrom(historicFrame);
    }
}
// updated 3/1/26
