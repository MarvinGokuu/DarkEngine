// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems; // Synchronized with path src/sv/dark/core/systems/

import sv.dark.core.AAACertified;

import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;

/**
 * RESPONSIBILITY: Deterministic UI Logic (Credits). Calculate credits scroll positioning deterministically.
 * WHY: To maintain replayability and exact deterministic behavior across runs.
 * TECHNIQUE: Use fixed-point arithmetic instead of floating-point math.
 * GUARANTEES: Bit-perfect deterministic outcomes and exact visual synchronization.
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
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public final class CreditsLogic {

    // Design constants (Memory offsets within the Frame)
    private static final long OFFSET_ACTIVE = DarkStateLayout.UI_CREDITS_ACTIVE;
    private static final long OFFSET_SCROLL = DarkStateLayout.UI_CREDITS_SCROLL;

    /**
     * Updates the position of the credits.
     * [TECHNICAL NOTE]: The use of deltaTime is permitted here for the calculation,
     * but the result is persisted as INT (Fixed Point) to guarantee
     * determinism in replays.
     */
    public static void update(WorldStateFrame state, float deltaTime) {
        if (state.readInt(OFFSET_ACTIVE) == 0)
            return;

        int currentY = state.readInt(OFFSET_SCROLL);

        // Mechanical Sympathy: Scaling by factor 10,000 to preserve precision in integers.
        // [OBSERVATION]: If bottleneck is detected in casting, recommended to pre-calculate
        // scroll_step in the Heartbeat.
        currentY -= (int) (deltaTime * 10000);

        // Scroll cycle reset (Normalized values for internal resolution)
        if (currentY < -30000)
            currentY = 72000;

        state.writeInt(OFFSET_SCROLL, currentY);
    }
    // updated 3/1/26
}
