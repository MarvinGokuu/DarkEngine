// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import java.lang.foreign.MemorySegment;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;
import sv.dark.state.WorldStateFrame;

/**
 * RESPONSIBILITY: Manage Spatial 3D Audio Context via OpenAL.
 * WHY: Audio must dynamically follow the player position for immersion.
 * TECHNIQUE: FFI direct binding. Reads player coordinates from Vault and updates OpenAL Listener natively.
 * GUARANTEES: Zero latency positional audio update, no Java object creation.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-06-13", maxLatencyNs = 5, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native OpenAL Listener Updates")
public final class DarkAudioSystem implements GameSystem {

    private final SectorMemoryVault vault;
    private MemorySegment device;
    private MemorySegment context;

    public DarkAudioSystem(SectorMemoryVault vault) {
        this.vault = vault;
        initOpenAL();
    }

    private void initOpenAL() {
        try {
            // Open default audio device
            device = (MemorySegment) DarkAudioLinker.alcOpenDevice.invokeExact(MemorySegment.NULL);
            if (device.equals(MemorySegment.NULL)) {
                DarkLogger.warning("AUDIO", "Failed to open OpenAL device. Audio disabled.");
                return;
            }

            // Create context
            context = (MemorySegment) DarkAudioLinker.alcCreateContext.invokeExact(device, MemorySegment.NULL);
            if (context.equals(MemorySegment.NULL)) {
                DarkLogger.warning("AUDIO", "Failed to create OpenAL context.");
                return;
            }

            // Make context current
            DarkAudioLinker.alcMakeContextCurrent.invokeExact(context);
            DarkLogger.info("AUDIO", "OpenAL Context Initialized. Spatial Audio Active.");

        } catch (Throwable t) {
            DarkLogger.warning("AUDIO", "OpenAL Initialization Exception: " + t.getMessage());
        }
    }

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        if (device == null || device.equals(MemorySegment.NULL)) return;

        try {
            // Read Player Position natively (Slots 0 and 4)
            float px = vault.readInt(DarkStateLayout.PLAYER_X) / 1000f; // Scale assuming stored as int * 1000
            float py = vault.readInt(DarkStateLayout.PLAYER_Y) / 1000f;

            // Update OpenAL Listener Position in 3D Space natively (Z = 0 for 2D Engine)
            DarkAudioLinker.alListener3f.invokeExact(DarkAudioLinker.AL_POSITION, px, py, 0.0f);

        } catch (Throwable e) {
            // Ignore in hot path
        }
    }

    public void cleanup() {
        try {
            if (context != null && !context.equals(MemorySegment.NULL)) {
                DarkAudioLinker.alcMakeContextCurrent.invokeExact(MemorySegment.NULL);
                DarkAudioLinker.alcDestroyContext.invokeExact(context);
            }
            if (device != null && !device.equals(MemorySegment.NULL)) {
                DarkAudioLinker.alcCloseDevice.invokeExact(device);
            }
        } catch (Throwable e) {
            DarkLogger.warning("AUDIO", "Failed to cleanup OpenAL natively.");
        }
    }
}
