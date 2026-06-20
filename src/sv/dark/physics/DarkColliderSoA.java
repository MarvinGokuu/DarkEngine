// Reading Order: 00100030
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import sv.dark.core.AAACertified;

/**
 * Data-Oriented Memory layout for Colliders (Phase 31).
 * 
 * Contiene los radios o bounding boxes de todas las entidades
 * en arreglos contiguos de memoria nativa off-heap para uso de AVX-512.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Memory for Bounds")
public final class DarkColliderSoA {
    
    public final MemorySegment radius; // Float (4 bytes)
    public final MemorySegment mass;   // Float (4 bytes)
    
    private final Arena arena;
    private final int capacity;

    public DarkColliderSoA(int capacity) {
        this.capacity = capacity;
        this.arena = Arena.ofShared();
        
        long bytesRequired = capacity * 4L;
        this.radius = arena.allocate(bytesRequired, 64);
        this.mass = arena.allocate(bytesRequired, 64);
    }

    public int getCapacity() {
        return capacity;
    }

    public void destroy() {
        if (arena.scope().isAlive()) {
            arena.close();
        }
    }
}
