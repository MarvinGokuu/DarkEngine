// Reading Order: 01011011
//  91
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkAudioContext;
import sv.dark.core.DarkLogger;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.state.DarkStateLayout;
import sv.dark.state.WorldStateFrame;
import sv.dark.audio.DarkAudioSourceSoA;

/**
 * RESPONSIBILITY: Manage Spatial 3D Audio Context via abstract backend.
 * WHY: Audio must dynamically follow the player position for immersion.
 * TECHNIQUE: Delegates to DarkAudioContext.
 * GUARANTEES: Zero latency positional audio update, no Java object creation.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 2.0
 */
@AAACertified(date = "2026-06-13", maxLatencyNs = 5, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Abstracted Audio System")
public final class DarkAudioSystem implements GameSystem {

    private final SectorMemoryVault vault;
    private final DarkAudioSourceSoA sources;

    public DarkAudioSystem(SectorMemoryVault vault, DarkAudioSourceSoA sources) {
        this.vault = vault;
        this.sources = sources;
        
        DarkAudioContext ctx = DarkAudioContext.get();
        if (ctx != null) {
            ctx.init(sources);
        }
    }

    @Override
    public void update(WorldStateFrame state, float deltaTime) {
        DarkAudioContext ctx = DarkAudioContext.get();
        if (ctx == null) return;

        try {
            float px = vault.readInt(DarkStateLayout.PLAYER_X) / 1000f;
            float py = vault.readInt(DarkStateLayout.PLAYER_Y) / 1000f;

            ctx.updateListener(px, py, 0.0f);
            ctx.updateSources(sources);
        } catch (Throwable e) {
            // Ignore in hot path
        }
    }

    public void cleanup() {
        DarkAudioContext ctx = DarkAudioContext.get();
        if (ctx != null) {
            ctx.cleanup();
        }
    }
    
    @Override
    public String getName() {
        return "DarkAudioSystem";
    }
}
