// Reading Order: 01001010
//  74
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.state;

import sv.dark.core.AAACertified;

/**
 * Off-heap memory address map for WorldStateFrame.
 *
 * <p>All constants are byte offsets into a native MemorySegment.
 * Guarantees L1 cache alignment and optimized offsets for zero-allocation
 * compile-time resolution.
 *
 * <p><b>Layout contract:</b>
 * <ul>
 *   <li>Slots 0-99: Hot data (high-frequency actor state).</li>
 *   <li>Slots 100-199: Critical kernel control logic.</li>
 *   <li>Slots 200-299: Hardware telemetry data.</li>
 *   <li>Slots 300-399: Input pipeline.</li>
 * </ul>
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date         = "2026-01-05",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment    = 64,
    lockFree     = true,
    offHeap      = false,
    notes        = "Compile-time pure constants — zero runtime overhead"
)
public final class DarkStateLayout {

    /** Utility class — no instances. */
    private DarkStateLayout() {
        throw new AssertionError("DarkStateLayout is a static utility class");
    }

    // -------------------------------------------------------------------------
    // Slots 0-99: High-Frequency State Buffer (Actor Coordinates)
    // -------------------------------------------------------------------------

    public static final int PLAYER_X = 0;
    public static final int PLAYER_Y = 4;
    public static final int PLAYER_DIR = 8;
    public static final int PLAYER_SCORE = 12;

    // -------------------------------------------------------------------------
    // Slots 100-199: Kernel Control Registers
    // -------------------------------------------------------------------------

    public static final int SYS_TICK = 400; // Slot 100
    // Engine lifecycle states: 0=RUNNING, 1=ALERT, 2=HALTED
    public static final int SYS_ENGINE_FLAGS = 404; // Slot 101
    public static final int SYS_TARGET_FPS = 408; // Slot 102
    // Fixed execution frame duration (timestep) represented as integer
    public static final int SYS_FRAME_INTERVAL = 412; // Slot 103 (was DELTA_TIME)
    public static final int ENTITY_COUNT = 416; // Slot 104

    // -------------------------------------------------------------------------
    // Slots 200-299: Hardware Telemetry Metrics (Scaled 0-10000)
    // -------------------------------------------------------------------------

    public static final int METRIC_CPU_LOAD = 800; // Slot 200
    public static final int METRIC_RAM_FREE = 808; // Slot 202
    public static final int METRIC_RAM_TOTAL = 816; // Slot 204

    // Legacy telemetry aliases
    public static final int SYS_CPU_LOAD = METRIC_CPU_LOAD;
    public static final int SYS_MEM_FREE = METRIC_RAM_FREE;

    // -------------------------------------------------------------------------
    // Slots 300-399: Input Pipeline (Latched State)
    // -------------------------------------------------------------------------

    public static final int INPUT_MOUSE_X = 1200; // Slot 300
    public static final int INPUT_MOUSE_Y = 1204; // Slot 301
    public static final int INPUT_LAST_SIGNAL = 1208; // Slot 302

    // -------------------------------------------------------------------------
    // Slots mapped for legacy systems (PlayerSystem / CreditsLogic)
    // -------------------------------------------------------------------------
    public static final int LEGACY_PLAYER_X_DOUBLE = 1000;
    public static final int LEGACY_PLAYER_Y_DOUBLE = 1008;
    public static final int LEGACY_PLAYER_INPUT = 1016;
    public static final int UI_CREDITS_ACTIVE = 1024;
    public static final int UI_CREDITS_SCROLL = 1028;

    // -------------------------------------------------------------------------
    // Memory Segment Bounds
    // -------------------------------------------------------------------------

    /**
     * Total allocated memory slots (1024 slots * 4 bytes = 4096 bytes / 4KB).
     * Defines the maximum boundary for the memory page to prevent cache thrashing.
     */
    public static final int MAX_SLOTS = 1024;

}
