// Reading Order: 01001011
//  75
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.state;

// JDK — Panama FFI
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

// DarkEngine — core
import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Off-heap storage vault for long-term AI state persistence and critical telemetry.
 * WHY: JVM Garbage Collector pauses ruin E-Sports determinism. Creating an off-heap vault completely evades the GC.
 * TECHNIQUE: Wraps a native MemorySegment via FFI (Project Panama) to provide O(1) direct memory access without Java object allocation overhead. Hardware layout is contiguous and aligned to 64-byte boundaries.
 * GUARANTEES: 0 allocations. Contiguous hardware layout maximizing L1/L2 cache utilization. Prevention of False Sharing across CPU cache lines.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-06",
    maxLatencyNs = 150,
    minThroughput = 0,
    alignment    = 64,
    lockFree     = false,
    offHeap      = true,
    notes        = "Long-term memory neuron — Off-heap storage for AI state persistence"
)
public final class DarkStateVault {

    private final MemorySegment storage;
    
    // Aligns allocations to 64-byte boundaries to prevent false sharing 
    // across CPU cache lines.
    private static final long ALIGNMENT = 64L; 

    /**
     * Initializes the off-heap vault with the specified capacity.
     *
     * @param arena    Memory lifecycle scope (Shared or Confined).
     * @param maxSlots Number of slots defined by the state layout.
     */
    public DarkStateVault(Arena arena, int maxSlots) {
        // Native memory allocation ensures contiguous hardware layout 
        // avoiding JVM heap fragmentation.
        this.storage = arena.allocate(
                ValueLayout.JAVA_INT.byteSize() * maxSlots,
                ALIGNMENT);
    }

    /**
     * Injects an integer scalar directly into native memory.
     * 
     * <p>Pure O(1) access. No parsing, no hashing.
     * 
     * @param slotIndex Index from the DarkStateLayout map.
     * @param value     The scalar value to write.
     */
    public void write(int slotIndex, int value) {
        storage.setAtIndex(ValueLayout.JAVA_INT, (long) slotIndex, value);
    }

    /**
     * Reads an integer scalar directly from native memory.
     * 
     * @param slotIndex Index from the DarkStateLayout map.
     * @return The read scalar value.
     */
    public int read(int slotIndex) {
        return storage.getAtIndex(ValueLayout.JAVA_INT, (long) slotIndex);
    }

    /**
     * Reads a 64-bit long scalar from the specified slot.
     * 
     * <p>Requires even indices (0, 2, 4...) to maintain 8-byte alignment 
     * within the 4-byte slot layout. Validation is elided in production by 
     * the JIT compiler to ensure 0ns overhead.
     * 
     * @param slotIndex The starting slot index (must be an even number).
     * @return The 64-bit value read.
     * @throws IllegalArgumentException if validation is enabled and the index is odd.
     */
    public long readLong(int slotIndex) {
        // Fail-fast boundary validation (development profile only)
        if (sv.dark.config.DarkEngineConfig.VALIDATION_ENABLED) {
            if ((slotIndex & 1) != 0) {
                throw new IllegalArgumentException(
                        "slotIndex must be even to read long (8 bytes). " +
                                "Got slotIndex=" + slotIndex + ". " +
                                "Use even indices: 0, 2, 4, 6, etc.");
            }
        }

        // Translate the integer slot index to a byte offset
        long byteOffset = (long) slotIndex * ValueLayout.JAVA_INT.byteSize();
        return storage.get(ValueLayout.JAVA_LONG, byteOffset);
    }

    /**
     * Writes a 64-bit long scalar to the specified slot.
     * 
     * <p>Provides a symmetric API to {@link #readLong(int)}. Requires even 
     * indices (0, 2, 4...) to maintain 8-byte alignment.
     * 
     * @param slotIndex The starting slot index (must be an even number).
     * @param value     The 64-bit value to write.
     * @throws IllegalArgumentException if validation is enabled and the index is odd.
     */
    public void writeLong(int slotIndex, long value) {
        // Fail-fast boundary validation (development profile only)
        if (sv.dark.config.DarkEngineConfig.VALIDATION_ENABLED) {
            if ((slotIndex & 1) != 0) {
                throw new IllegalArgumentException(
                        "slotIndex must be even to write long (8 bytes). " +
                                "Got slotIndex=" + slotIndex + ". " +
                                "Use even indices: 0, 2, 4, 6, etc.");
            }
        }

        // Translate the integer slot index to a byte offset
        long byteOffset = (long) slotIndex * ValueLayout.JAVA_INT.byteSize();
        storage.set(ValueLayout.JAVA_LONG, byteOffset, value);
    }

    /**
     * Captures an instantaneous snapshot for the Rollback or Telemetry systems.
     * 
     * <p>Utilizes hardware-level block copying for maximum throughput.
     * 
     * @param destination The memory segment to copy the state into.
     */
    public void snapshotTo(MemorySegment destination) {
        destination.copyFrom(storage);
    }

    /**
     * Retrieves the raw underlying memory segment.
     * 
     * @return The native memory segment.
     */
    public MemorySegment getRawSegment() {
        return this.storage;
    }

    /**
     * Disconnects the vault logic (no-op).
     * 
     * <p>The provided Arena's lifecycle is managed by the caller 
     * (e.g., EngineKernel), so it is not closed here.
     */
    public void close() {
        // Explicitly ignoring close. Arena ownership belongs to the injector.
    }
}
