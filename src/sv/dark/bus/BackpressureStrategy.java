// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: Define bus saturation handling strategies.
 * WHY: High-throughput event systems must define deterministic policies for when the ring buffer reaches capacity.
 * TECHNIQUE: Strategy Pattern Enum defining DROP, BLOCK, and OVERWRITE policies.
 * GUARANTEES: Deterministic behavior under backpressure.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(date = "2026-06-11", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = false, offHeap = false, notes = "Automatically AAA Certified during Core Audit")
public enum BackpressureStrategy {

    /**
     * Drops the new event if the bus is full.
     * Use: Non-critical events (e.g., particles, visual effects).
     */
    DROP,

    /**
     * Blocks the thread until space is available.
     * Use: Critical events that cannot be lost (e.g., player input).
     * WARNING: Can cause a deadlock if the bus is not consumed.
     */
    BLOCK,

    /**
     * Overwrites the oldest event with the new one.
     * Use: Events where only the most recent state matters (e.g., mouse
     * position).
     */
    OVERWRITE
}
