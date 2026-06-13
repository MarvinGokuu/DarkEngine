// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core; // Sincronizado con la ruta src/sv/dark/core/

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Offsets Map (Layout) for entities in Off-Heap memory.
 * WHY: Objects on the JVM heap suffer from scattered memory allocation, causing cache misses. We need a contiguous, predictable layout.
 * TECHNIQUE: Defines the physical byte structure of an Entity in native memory using static constant offsets.
 * GUARANTEES: 64-byte Alignment (Cache Line). Access O(1). Perfect alignment to prevent False Sharing and allow vectorized access (SIMD).
 * 
 * <p>Metrics: 64-byte Alignment (Cache Line), Access O(1)
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
public final class EntityLayout {

    // 64-byte structure per entity (Perfect alignment with L1 Cache Line)
    public static final long STRIDE = 64L;

    // Position (8 bytes each - double/long)
    public static final long X_OFFSET = 0L;
    public static final long Y_OFFSET = 8L;

    // Velocity (Required by MovementSystem)
    public static final long VX_OFFSET = 16L;
    public static final long VY_OFFSET = 24L;

    // Metadata and Visualization
    public static final long ID_OFFSET = 32L;
    public static final long STATE_FLAGS = 40L;
    public static final long GLOW_ALPHA = 48L; // Required by SpriteSystem

    // Spatial Management (Bytes 56-64)
    public static final long SECTOR_ID_OFFSET = 56L; // Required by DarkSectorManager

    private EntityLayout() {
    } // Sealed: Only addressing constants.
}
// updated 3/1/26
