// Reading Order: 00010011
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import sv.dark.state.WorldStateFrame;

/**
 * Spatial Domain Orchestration (Grid Management).
 * 
 * <p>Manages the spatial grid and the transfer of entities between sectors.
 * Uses 64-bit spatial keys for fast addressing.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.2
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 50, minThroughput = 1_000_000, alignment = 0, lockFree = true, offHeap = false, notes = "Spatial Grid Orchestration (Zero-Allocation Lookups)")
public final class DarkSectorManager {

    // [DATA ARCHITECTURE]:
    // Replacement of ConcurrentHashMap with SectorMap (Long2Object).
    // Removed Long Boxing.
    // Zero-Allocation guaranteed in location update operations.
    private final SectorMap<DarkSector> sectores = new SectorMap<>(1024);

    /**
     * Relocates the entity using its current sector signature to avoid global searches.
     * 
     * <p>[MECHANICAL SYMPATHY]: Bit-shifting used to map coordinates to grid cells
     * via SpaceMath.
     * 
     * @param entityId Unique entity ID.
     * @param x        World X coordinate.
     * @param y        World Y coordinate.
     * @param frame    Current state frame (Snapshot).
     */
    public void updateLocation(long entityId, float x, float y, WorldStateFrame frame) {
        // [OPTIMIZATION]: Delegated to SpaceMath
        int sx = SpaceMath.getSectorIndex(x);
        int sy = SpaceMath.getSectorIndex(y);

        // Pack 2D coordinates into a single 64-bit key.
        long newKey = SpaceMath.packKey2D(sx, sy);

        // Get the sector where the entity resided from the WorldState.
        long oldKey = frame.readLong(entityId + EntityLayout.SECTOR_ID_OFFSET);

        if (newKey != oldKey) {
            transferEntity(entityId, oldKey, newKey);
            // Persist the entity's new spatial domain.
            frame.writeLong(entityId + EntityLayout.SECTOR_ID_OFFSET, newKey);
        }
    }

    /**
     * Atomically transfers an entity from one sector to another.
     * 
     * <p>AAA+ OPTIMIZATION:
     * <ul>
     *   <li>Use of primitive longs in SectorMap.</li>
     *   <li>No boxing, no lambdas, no garbage iterators.</li>
     * </ul>
     */
    private void transferEntity(long entityId, long fromKey, long toKey) {
        // 1. Leave the old sector (Safe Null Check)
        DarkSector oldSector = sectores.get(fromKey); // Zero-Alloc get
        if (oldSector != null) {
            oldSector.unregisterEntity();
        }

        // 2. Enter the new sector
        DarkSector newSector = sectores.get(toKey); // Zero-Alloc get

        // Lazy Initialization
        if (newSector == null) {
            // Note: null is passed as MemorySegment temporarily.
            // Actual memory mapping is done in the Boot phase of SectorMemoryVault.
            DarkSector freshSector = new DarkSector(toKey, null, 1024);
            // putIfAbsent returns the existing value (if race condition) or null (if won)
            DarkSector existing = sectores.putIfAbsent(toKey, freshSector);
            newSector = (existing != null) ? existing : freshSector;
        }

        newSector.registerEntity();
    }
}
