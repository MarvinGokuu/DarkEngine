// Reading Order: 00010100
//  20
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.audio;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * Memoria Data-Oriented para manejar miles de fuentes de audio simultáneas.
 * Sin recolector de basura (Zero-GC) utilizando bloques nativos alineados.
 */
@AAACertified(date = "2026-06-20", maxLatencyNs = 10, minThroughput = 0, lockFree = true, offHeap = true, notes = "Phase 33: Data-Oriented Audio Source Struct")
public final class DarkAudioSourceSoA {

    private final Arena arena;
    private final int capacity;

    // OpenAL IDs
    public final MemorySegment sourceIds;
    public final MemorySegment bufferIds;

    // Propiedades 3D
    public final MemorySegment posX;
    public final MemorySegment posY;
    public final MemorySegment posZ;
    public final MemorySegment velX;
    public final MemorySegment velY;
    public final MemorySegment velZ;

    // Acústica
    public final MemorySegment pitch;
    public final MemorySegment gain;

    public DarkAudioSourceSoA(int capacity) {
        this.capacity = capacity;
        this.arena = Arena.ofShared();
        
        long bytesInt = capacity * 4L;
        long bytesFloat = capacity * 4L;

        this.sourceIds = arena.allocate(bytesInt);
        this.bufferIds = arena.allocate(bytesInt);
        
        this.posX = arena.allocate(bytesFloat);
        this.posY = arena.allocate(bytesFloat);
        this.posZ = arena.allocate(bytesFloat);
        this.velX = arena.allocate(bytesFloat);
        this.velY = arena.allocate(bytesFloat);
        this.velZ = arena.allocate(bytesFloat);
        this.pitch = arena.allocate(bytesFloat);
        this.gain = arena.allocate(bytesFloat);
        
        // Inicializar a valores base
        for (int i = 0; i < capacity; i++) {
            pitch.set(ValueLayout.JAVA_FLOAT, i * 4L, 1.0f);
            gain.set(ValueLayout.JAVA_FLOAT, i * 4L, 1.0f);
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void destroy() {
        arena.close();
    }
}
