// Reading Order: 00000101
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * Functional domain classification for events.
 *
 * <p>Provides separation of concerns, allowing specialized lanes.
 * Uses ordinal values for O(1) array lookups in the dispatcher.
 * 
 * <p>PATTERN: Type-Safe Enum
 * <br>ROLE: Event Classification
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(
    date         = "2026-01-04",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment    = 0,
    lockFree     = true,
    offHeap      = false,
    notes        = "Ordinal-based classification for O(1) dispatching"
)
public enum DarkEventType {

    /** User input events (Keyboard, Mouse, Gamepad). */
    INPUT(0x1000),

    /** Network events (Packets, State Sync). */
    NETWORK(0x2000),

    /** System events (Entity spawn, Engine state changes). */
    SYSTEM(0x3000),

    /** Audio events (Play sound, volume changes). */
    AUDIO(0x4000),

    /** Physics events (Collisions, forces). */
    PHYSICS(0x5000),

    /** Render events (Shader changes, textures). */
    RENDER(0x6000);

    private final int baseId;

    // Cache the values array to avoid reallocation on values() call
    private static final DarkEventType[] VALUES = values();

    DarkEventType(int baseId) {
        this.baseId = baseId;
    }

    /**
     * Returns the base ID of the event type.
     * Used for packing events with their type included.
     * 
     * @return Base ID (upper 16 bits of the command ID).
     */
    public int getBaseId() {
        return baseId;
    }

    /**
     * Extracts the event type from a command ID.
     * 
     * @param commandId Command ID (32 bits).
     * @return Corresponding event type.
     */
    public static DarkEventType fromCommandId(int commandId) {
        int typeId = commandId & 0xF000;
        
        // Mechanical Sympathy: Switch is faster than loop for small dense sets
        switch(typeId) {
            case 0x1000: return INPUT;
            case 0x2000: return NETWORK;
            case 0x3000: return SYSTEM;
            case 0x4000: return AUDIO;
            case 0x5000: return PHYSICS;
            case 0x6000: return RENDER;
            default:     return SYSTEM;
        }
    }

    /**
     * Mechanical Sympathy: Fast retrieval of all values.
     * 
     * @return Cached array of event types.
     */
    public static DarkEventType[] cachedValues() {
        return VALUES;
    }
}
