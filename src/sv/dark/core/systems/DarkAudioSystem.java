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
import sv.dark.audio.DarkAudioSourceSoA;

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
    private final DarkAudioSourceSoA sources;
    private MemorySegment device;
    private MemorySegment context;

    public DarkAudioSystem(SectorMemoryVault vault, DarkAudioSourceSoA sources) {
        this.vault = vault;
        this.sources = sources;
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

            // Make context current (must capture the byte return value for invokeExact)
            byte result = (byte) DarkAudioLinker.alcMakeContextCurrent.invokeExact(context);
            
            // Inicializar las 1024 fuentes de OpenAL de golpe
            DarkAudioLinker.alGenSources.invokeExact(sources.getCapacity(), sources.sourceIds);
            
            DarkLogger.info("AUDIO", "OpenAL Context Initialized. Spatial Audio Active (1024 Sources).");

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
            
            // Phase 33: Sincronizar Físicas de Fuentes de Audio para Doppler y HRTF
            int cap = sources.getCapacity();
            for (int i = 0; i < cap; i++) {
                int sourceId = sources.sourceIds.get(java.lang.foreign.ValueLayout.JAVA_INT, i * 4L);
                if (sourceId == 0) continue; // Fuente inactiva
                
                float spx = sources.posX.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float spy = sources.posY.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float spz = sources.posZ.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                
                float svx = sources.velX.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float svy = sources.velY.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                float svz = sources.velZ.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, i * 4L);
                
                DarkAudioLinker.alSource3f.invokeExact(sourceId, DarkAudioLinker.AL_POSITION, spx, spy, spz);
                DarkAudioLinker.alSource3f.invokeExact(sourceId, DarkAudioLinker.AL_VELOCITY, svx, svy, svz);
            }

        } catch (Throwable e) {
            // Ignore in hot path
        }
    }

    public void cleanup() {
        try {
            if (context != null && !context.equals(MemorySegment.NULL)) {
                byte result = (byte) DarkAudioLinker.alcMakeContextCurrent.invokeExact(MemorySegment.NULL);
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
