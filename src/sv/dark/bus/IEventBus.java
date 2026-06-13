// Reading Order: 00000011
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * Standard communication contract for zero-allocation event buses.
 *
 * <p>Enforces a primitive-only (64-bit long) API to prevent object allocation
 * on the hot path. Implementations may utilize ring buffers, atomic arrays,
 * or LMAX-style architectures behind this abstraction.
 *
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(
    date         = "2026-01-04",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment    = 64,
    lockFree     = true,
    offHeap      = false,
    notes        = "Interface contract — primitive only to avoid GC pressure"
)
public interface IEventBus {

    /**
     * Attempts to insert an event into the bus without blocking.
     * 
     * @param event The encoded event payload (64 bits).
     * @return {@code true} if accepted, {@code false} if the buffer is saturated.
     */
    boolean offer(long event);

    /**
     * Consumes and removes the next event from the bus.
     * 
     * @return The next event, or {@code -1L} if the bus is empty.
     */
    long poll();

    /**
     * Reads the next event without consuming it.
     * 
     * @return The next event, or {@code -1L} if the bus is empty.
     */
    long peek();

    /**
     * Retrieves the current number of pending events.
     * 
     * @return Total events available for consumption.
     */
    int size();

    /**
     * Retrieves the maximum physical capacity of the bus.
     * 
     * @return Maximum number of events the bus can store.
     */
    int capacity();

    /**
     * Calculates the remaining available capacity.
     * 
     * @return Number of events that can still be offered.
     */
    default int remainingCapacity() {
        return capacity() - size();
    }

    /**
     * Flushes all pending events from the bus destructively.
     */
    void clear();

    /**
     * Checks if the bus contains no pending events.
     * 
     * @return {@code true} if empty.
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Checks if the bus has reached its maximum capacity.
     * 
     * @return {@code true} if no more events can be offered.
     */
    default boolean isFull() {
        return remainingCapacity() == 0;
    }

    /**
     * Retrieves the hardware latency of the last transaction.
     * 
     * @return Latency in nanoseconds. Defaults to 0L if unsupported.
     */
    default long getLastLatencyNs() {
        return 0L;
    }
}
