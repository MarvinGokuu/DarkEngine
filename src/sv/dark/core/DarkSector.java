// Reading Order: 00000111
//  7
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core; // Sincronizado con la ruta f­sica src/sv/dark/core/systems/

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spatial Memory Abstraction (Sliver).
 * 
 * <p>Represents a physical sector of the world mapped to a memory segment.
 * It does not own the memory, only the reference (View) for spatial operations.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 10, minThroughput = 100_000_000, alignment = 0, lockFree = true, offHeap = true, notes = "Spatial memory abstraction (Zero-Copy View)")
public final class DarkSector {

    private final long sectorHash;
    private final MemorySegment sectorMemory;
    private final int maxEntities;
    private final AtomicInteger activeCount;

    /**
     * The sector receives a portion (Sliver) of the total native memory of the Vault.
     * [MECHANICAL SYMPATHY]: No new memory is allocated, an existing reference is mapped.
     */
    public DarkSector(long sectorHash, MemorySegment rawVaultSegment, int maxEntities) {
        this.sectorHash = sectorHash;
        this.maxEntities = maxEntities;
        this.sectorMemory = rawVaultSegment; // Reference to the dedicated memory block (Zero-Copy)
        this.activeCount = new AtomicInteger(0);
    }

    /**
     * In the core model, "registering" is simply incrementing the pointer
     * of active entities within the pre-allocated memory segment.
     */
    public void registerEntity() {
        while (true) {
            int current = activeCount.get();
            if (current >= maxEntities) break;
            if (activeCount.compareAndSet(current, current + 1)) break;
        }
    }

    /**
     * Decrements the count of active entities when leaving the sector.
     */
    public void unregisterEntity() {
        while (true) {
            int current = activeCount.get();
            if (current <= 0) break;
            if (activeCount.compareAndSet(current, current - 1)) break;
        }
    }

    public MemorySegment getSectorMemory() {
        return sectorMemory;
    }

    public int getActiveCount() {
        return activeCount.get();
    }

    public long getSectorHash() {
        return sectorHash;
    }
}
// updated 3/1/26
