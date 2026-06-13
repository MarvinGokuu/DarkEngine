// Reading Order: 00001001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.bus;

import sv.dark.core.AAACertified;

/**
 * Central Event Orchestrator and Multi-Lane Dispatcher.
 *
 * <p>Main facade for the event system. Manages multiple specialized lanes 
 * for different traffic types (Network, Physics, System, etc.) with independent
 * backpressure strategies.
 * 
 * <p>MECHANICAL SYMPATHY: Uses direct array indexing (O(1)) instead of HashMaps 
 * to achieve zero-allocation routing and eliminate L1 cache misses.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-05",
    maxLatencyNs = 50,
    minThroughput = 5_000_000,
    alignment    = 64,
    lockFree     = true,
    offHeap      = false,
    notes        = "Zero-Allocation Dispatch. O(1) Array Routing instead of HashMap"
)
public final class DarkEventDispatcher {

    // Mechanical Sympathy: Direct array mapping based on Enum Ordinal
    private final DarkEventLane[] laneArray;
    
    // Priority order for sequential processing
    private static final DarkEventType[] PRIORITY_ORDER = {
        DarkEventType.SYSTEM,
        DarkEventType.NETWORK,
        DarkEventType.INPUT,
        DarkEventType.PHYSICS,
        DarkEventType.AUDIO,
        DarkEventType.RENDER
    };

    /**
     * Creates an empty dispatcher.
     * Lanes must be registered manually with registerLane().
     */
    public DarkEventDispatcher() {
        this.laneArray = new DarkEventLane[DarkEventType.cachedValues().length];
    }

    /**
     * Creates a dispatcher with predefined lanes.
     * 
     * @param busSize Size of each bus (power of 2).
     * @return Fully configured dispatcher.
     */
    public static DarkEventDispatcher createDefault(int busSize) {
        DarkEventDispatcher dispatcher = new DarkEventDispatcher();

        // Input Lane: DROP (high frequency, non-critical)
        dispatcher.registerLane(
                DarkEventType.INPUT,
                new DarkRingBus(busSize),
                BackpressureStrategy.DROP);

        // Network Lane: BLOCK (critical, must not be lost)
        dispatcher.registerLane(
                DarkEventType.NETWORK,
                new DarkRingBus(busSize),
                BackpressureStrategy.BLOCK);

        // System Lane: BLOCK (critical engine events)
        dispatcher.registerLane(
                DarkEventType.SYSTEM,
                new DarkRingBus(busSize),
                BackpressureStrategy.BLOCK);

        // Audio Lane: DROP (non-critical)
        dispatcher.registerLane(
                DarkEventType.AUDIO,
                new DarkRingBus(busSize),
                BackpressureStrategy.DROP);

        // Physics Lane: OVERWRITE (only most recent state matters)
        dispatcher.registerLane(
                DarkEventType.PHYSICS,
                new DarkRingBus(busSize),
                BackpressureStrategy.OVERWRITE);

        // Render Lane: DROP (visual events, non-critical)
        dispatcher.registerLane(
                DarkEventType.RENDER,
                new DarkRingBus(busSize),
                BackpressureStrategy.DROP);

        return dispatcher;
    }

    // -------------------------------------------------------------------------
    // LANE REGISTRATION
    // -------------------------------------------------------------------------

    /**
     * Registers a specialized lane using O(1) ordinal mapping.
     * 
     * @param type     Event type (determines array index).
     * @param bus      Bus implementation.
     * @param strategy Backpressure strategy.
     */
    public void registerLane(DarkEventType type, IEventBus bus, BackpressureStrategy strategy) {
        DarkEventLane lane = new DarkEventLane(type.name(), type, bus, strategy);
        laneArray[type.ordinal()] = lane;
    }

    /**
     * Retrieves a lane by event type.
     * 
     * @param type Event type.
     * @return Lane or null if it doesn't exist.
     */
    public DarkEventLane getLane(DarkEventType type) {
        return laneArray[type.ordinal()];
    }

    // -------------------------------------------------------------------------
    // EVENT DISPATCH
    // -------------------------------------------------------------------------

    /**
     * Dispatches an event to a specific lane via O(1) lookup.
     * 
     * @param type  Event type (target lane).
     * @param event Encoded event.
     * @return true if accepted.
     */
    public boolean dispatch(DarkEventType type, long event) {
        DarkEventLane lane = laneArray[type.ordinal()];
        if (lane == null) {
            return false;
        }
        return lane.offer(event);
    }

    /**
     * Automatically routes an event by extracting its type from the command ID.
     * 
     * @param event Encoded event (must include type in command ID).
     * @return true if accepted.
     */
    public boolean dispatchAuto(long event) {
        int commandId = DarkSignalPacker.unpackCommandId(event);
        DarkEventType type = DarkEventType.fromCommandId(commandId);
        
        DarkEventLane lane = laneArray[type.ordinal()];
        if (lane != null) {
            return lane.offer(event);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // EVENT PROCESSING
    // -------------------------------------------------------------------------

    /**
     * Processes all events in a specific lane.
     * 
     * @param type      Event type.
     * @param processor Processing function.
     * @return Number of processed events.
     */
    public int processLane(DarkEventType type, java.util.function.LongConsumer processor) {
        DarkEventLane lane = laneArray[type.ordinal()];
        if (lane == null) {
            return 0;
        }
        return lane.processAll(processor);
    }

    /**
     * Processes all events in all lanes based on structural priority.
     * 
     * @param processor Processing function.
     * @return Total number of processed events.
     */
    public int processAll(java.util.function.LongConsumer processor) {
        int total = 0;

        for (int i= 0; i< PRIORITY_ORDER.length; i++) {
            DarkEventType type = PRIORITY_ORDER[i];
            DarkEventLane lane = laneArray[type.ordinal()];
            if (lane != null) {
                total += lane.processAll(processor);
            }
        }

        return total;
    }

    public int batchPollAll(long[] buffer) {
        int total = 0;

        for (int i= 0; i< PRIORITY_ORDER.length; i++) {
            DarkEventType type = PRIORITY_ORDER[i];
            DarkEventLane lane = laneArray[type.ordinal()];
            if (lane != null) {
                int remaining = buffer.length - total;
                if (remaining == 0) break;
                
                long event;
                while (total < buffer.length && (event = lane.poll()) != -1) {
                    buffer[total++] = event;
                }
            }
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // OBSERVABILITY
    // -------------------------------------------------------------------------

    /**
     * Prints the status of all active lanes.
     */
    public void printStatus() {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  DARK EVENT DISPATCHER - STATUS REPORT");
        System.out.println("═══════════════════════════════════════════════════════");

        for (int i= 0; i< laneArray.length; i++) {
            DarkEventLane lane = laneArray[i];
            if (lane != null) {
                System.out.println(lane.getStatusReport());
            }
        }

        System.out.println("═══════════════════════════════════════════════════════");
    }

    /**
     * Clears all active lanes.
     */
    public void clearAll() {
        for (int i= 0; i< laneArray.length; i++) {
            DarkEventLane lane = laneArray[i];
            if (lane != null) {
                lane.clear();
            }
        }
    }

    /**
     * Returns the total number of pending events across all lanes.
     * 
     * @return Sum of pending events.
     */
    public int getTotalPendingEvents() {
        int total = 0;
        for (int i= 0; i< laneArray.length; i++) {
            DarkEventLane lane = laneArray[i];
            if (lane != null) {
                total += lane.size();
            }
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // GRACEFUL SHUTDOWN
    // -------------------------------------------------------------------------

    /**
     * Safe shutdown of the dispatcher and underlying buses.
     * 
     * <p>PURPOSE:
     * - Close all priority buses safely.
     * - Inject Tombstone Events.
     * - Release bus references.
     */
    public void shutdown() {
        System.out.println("[EVENT DISPATCHER] Initiating shutdown for all lanes...");

        int pendingEvents = getTotalPendingEvents();
        if (pendingEvents > 0) {
            System.err.printf("[EVENT DISPATCHER] WARNING: %d pending events at shutdown%n", pendingEvents);
        }

        // Close in reverse priority order
        for (int i= PRIORITY_ORDER.length - 1; i>= 0; i--) {
            DarkEventType type = PRIORITY_ORDER[i];
            DarkEventLane lane = laneArray[type.ordinal()];
            
            if (lane != null) {
                IEventBus bus = lane.getBus();
                if (bus instanceof DarkAtomicBus) {
                    ((DarkAtomicBus) bus).gracefulShutdown();
                } else if (bus instanceof DarkRingBus) {
                    ((DarkRingBus) bus).gracefulShutdown();
                } else {
                    bus.clear();
                }
                
                // Release reference
                laneArray[type.ordinal()] = null;
            }
        }

        System.out.println("[EVENT DISPATCHER] Shutdown completed");
    }
}
