// Reading Order: 00001000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * Specialized Event Lane with integrated metrics and backpressure.
 *
 * <p>Provides full observability and deterministic handling of bus saturation.
 * Enforces zero-allocation strictly on the hot-path (offer/poll).
 * 
 * <p>PATTERN: Decorator + Strategy
 * <br>ROLE: Specialized Event Channel
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(
    date         = "2026-06-12",
    maxLatencyNs = 100,
    minThroughput = 20_000_000,
    alignment    = 64,
    lockFree     = true,
    offHeap      = false,
    notes        = "Optimized for single-thread metrics using primitive long fields and infinite BLOCK retries"
)
public final class DarkEventLane {

    private final IEventBus bus;
    private final String name;
    private final DarkEventType type;
    private final BackpressureStrategy strategy;

    // -------------------------------------------------------------------------
    // METRICS (Zero-Allocation Primitive Counters)
    // -------------------------------------------------------------------------

    // Padding to prevent False Sharing (L1 Cache Line = 64 bytes)
    private long headShield_L1_slot1, headShield_L1_slot2, headShield_L1_slot3,
            headShield_L1_slot4, headShield_L1_slot5, headShield_L1_slot6,
            headShield_L1_slot7; // 7 slots × 8 bytes = 56 bytes

    private long totalOffered = 0;
    private long totalAccepted = 0;
    private long totalDropped = 0;

    // Inter-thread padding to isolate producer metrics from consumer metrics
    private long midShield_L1_slot1, midShield_L1_slot2, midShield_L1_slot3,
            midShield_L1_slot4, midShield_L1_slot5, midShield_L1_slot6,
            midShield_L1_slot7; // 7 slots × 8 bytes = 56 bytes

    private long totalPolled = 0;

    // Tail padding to prevent false sharing at the end of the object
    private long tailShield_L1_slot1, tailShield_L1_slot2, tailShield_L1_slot3,
            tailShield_L1_slot4, tailShield_L1_slot5, tailShield_L1_slot6,
            tailShield_L1_slot7; // 7 slots × 8 bytes = 56 bytes

    /**
     * Creates a specialized lane with a backpressure strategy.
     * 
     * @param name     Lane name (e.g., "Input", "Network").
     * @param type     Type of events it handles.
     * @param bus      Underlying bus implementation.
     * @param strategy Backpressure strategy.
     */
    public DarkEventLane(String name, DarkEventType type, IEventBus bus, BackpressureStrategy strategy) {
        this.name = name;
        this.type = type;
        this.bus = bus;
        this.strategy = strategy;
    }

    // -------------------------------------------------------------------------
    // CORE OPERATIONS
    // -------------------------------------------------------------------------

    /**
     * Offers an event to the lane with backpressure handling.
     * 
     * @param event Encoded event.
     * @return true if the event was accepted.
     */
    public boolean offer(long event) {
        totalOffered++;

        boolean accepted = bus.offer(event);

        if (accepted) {
            totalAccepted++;
            return true;
        }

        // Handle backpressure according to strategy
        switch (strategy) {
            case DROP:
                totalDropped++;
                return false;

            case BLOCK:
                // Infinite spin-wait retry to avoid discarding vital core signals
                while (!bus.offer(event)) {
                    if (Thread.currentThread().isInterrupted()) {
                        totalDropped++;
                        return false;
                    }
                    Thread.onSpinWait(); // CPU hint to reduce power consumption and allow memory sync
                }
                totalAccepted++;
                return true;

            case OVERWRITE:
                // Discard the oldest event and retry
                bus.poll();
                totalDropped++;
                boolean retryAccepted = bus.offer(event);
                if (retryAccepted) {
                    totalAccepted++;
                }
                return retryAccepted;

            default:
                totalDropped++;
                return false;
        }
    }

    /**
     * Consumes the next event from the lane.
     * 
     * @return The event or -1 if empty.
     */
    public long poll() {
        long event = bus.poll();
        if (event != -1) {
            totalPolled++;
        }
        return event;
    }

    /**
     * Reads the next event without consuming it.
     * 
     * @return The event or -1 if empty.
     */
    public long peek() {
        return bus.peek();
    }

    /**
     * Processes all available events in the lane.
     * 
     * @param processor Function that processes each event.
     * @return Number of events processed.
     */
    public int processAll(java.util.function.LongConsumer processor) {
        int count = 0;
        long event;
        while ((event = poll()) != -1) {
            processor.accept(event);
            count++;
        }
        return count;
    }

    /**
     * Extracts up to buffer.length events into a primitive array (Zero-Allocation).
     * @param buffer Output array.
     * @return Number of events extracted.
     */
    public int batchPollAll(long[] buffer) {
        int count = 0;
        long event;
        while (count < buffer.length && (event = poll()) != -1) {
            buffer[count++] = event;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // OBSERVABILITY
    // -------------------------------------------------------------------------

    public String getName() { return name; }
    public DarkEventType getType() { return type; }
    public IEventBus getBus() { return bus; }
    
    public int size() { return bus.size(); }
    public int capacity() { return bus.capacity(); }
    public int remainingCapacity() { return bus.remainingCapacity(); }
    public boolean isEmpty() { return bus.isEmpty(); }
    public boolean isFull() { return bus.isFull(); }

    public long getTotalOffered() { return totalOffered; }
    public long getTotalAccepted() { return totalAccepted; }
    public long getTotalDropped() { return totalDropped; }
    public long getTotalPolled() { return totalPolled; }

    /**
     * Returns the lane acceptance rate (0.0 to 1.0).
     * 
     * @return Percentage of accepted events.
     */
    public double getAcceptanceRate() {
        long offered = totalOffered;
        if (offered == 0) return 1.0;
        return (double) totalAccepted / offered;
    }

    /**
     * Returns the lane drop rate (0.0 to 1.0).
     * 
     * @return Percentage of dropped events.
     */
    public double getDropRate() {
        long offered = totalOffered;
        if (offered == 0) return 0.0;
        return (double) totalDropped / offered;
    }

    /**
     * Clears the lane and resets metrics.
     */
    public void clear() {
        bus.clear();
        totalOffered = 0;
        totalAccepted = 0;
        totalDropped = 0;
        totalPolled = 0;
    }

    /**
     * Returns the checksum of the padding variables.
     * 
     * @return Padding checksum (should be 0 under normal conditions).
     */
    public long getPaddingChecksum() {
        long acc = 0;
        acc += headShield_L1_slot1 + headShield_L1_slot2 + headShield_L1_slot3 +
               headShield_L1_slot4 + headShield_L1_slot5 + headShield_L1_slot6 + headShield_L1_slot7;
               
        acc += midShield_L1_slot1 + midShield_L1_slot2 + midShield_L1_slot3 +
               midShield_L1_slot4 + midShield_L1_slot5 + midShield_L1_slot6 + midShield_L1_slot7;
               
        acc += tailShield_L1_slot1 + tailShield_L1_slot2 + tailShield_L1_slot3 +
               tailShield_L1_slot4 + tailShield_L1_slot5 + tailShield_L1_slot6 + tailShield_L1_slot7;
        return acc;
    }

    /**
     * Generates a state report of the lane.
     * 
     * @return String containing lane metrics.
     */
    public String getStatusReport() {
        return String.format(
                "[LANE: %s] Type=%s | Size=%d/%d | Offered=%d | Accepted=%d | Dropped=%d | Rate=%.2f%%",
                name, type, size(), capacity(), totalOffered, totalAccepted, totalDropped, getAcceptanceRate() * 100);
    }
}
