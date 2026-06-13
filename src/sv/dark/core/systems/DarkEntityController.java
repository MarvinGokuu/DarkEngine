// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems; // Synchronized with physical location in src/sv/dark/bus/

import sv.dark.core.AAACertified;

import sv.dark.state.DarkStateVault;
import sv.dark.state.DarkStateLayout;

/**
 * RESPONSIBILITY: Binary Command Dispatch (ABI). Execute logic directly on the Vault using numeric command IDs.
 * WHY: To provide an ABI-based deterministic state machine for entity control without object instantiation.
 * TECHNIQUE: Switch on integer command IDs and directly write to the memory Vault using predefined offsets.
 * GUARANTEES: O(1) Command Dispatch and 100% deterministic execution.
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
public final class DarkEntityController {

    // Command IDs (Engine ABI)
    public static final int CMD_MOVE_SPRITE = 0x01;
    public static final int CMD_SCALE_SPRITE = 0x02;
    public static final int CMD_RESET_STATE = 0xFF;

    /**
     * Processes the action using the registry table (Vault) and the command ID.
     * [TECHNICAL NOTE]: Determinism depends on the vault being the only source
     * of input.
     */
    public static void dispatch(int commandId, DarkStateVault vault) {

        switch (commandId) {
            case CMD_MOVE_SPRITE -> {
                // Use the current TICK as a seed for basic determinism
                int tick = vault.read(DarkStateLayout.SYS_TICK);

                // Deterministic pseudo-random position generation
                // [OBSERVATION]: Modulo (%) operations detected.
                // Pending optimization to bitwise AND if latency is detected in massive simulation.
                int nx = (tick * 13) % 800;
                int ny = (tick * 7) % 600;

                // Direct write to native memory (State Control)
                vault.write(DarkStateLayout.PLAYER_X, nx);
                vault.write(DarkStateLayout.PLAYER_Y, ny);
            }

            case CMD_RESET_STATE -> {
                vault.write(DarkStateLayout.PLAYER_X, 0);
                vault.write(DarkStateLayout.PLAYER_Y, 0);
            }

            default -> {
                // Report unknown command in the flags registry
                // [PROTOCOL VIOLATION V2.0]: The error code should be in an error Layout.
                vault.write(DarkStateLayout.SYS_ENGINE_FLAGS, 0x02); // Alert flag
            }
        }
    }
    // updated 3/1/26
}
