// Reading Order: 00100040
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.vfx;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * CPU-Side Emitter configurations.
 * Almacena la configuración de los emisores (No de las partículas individuales).
 * Las partículas individuales viven enteramente en la VRAM (Tarjeta Gráfica).
 */
@AAACertified(date = "2026-06-20", maxLatencyNs = 5, minThroughput = 0, lockFree = true, offHeap = true, notes = "Phase 32: CPU-side Emitter Descriptor")
public final class DarkParticleEmitterSoA {

    private final Arena arena;
    private final int capacity;

    // Emisor X/Y
    public final MemorySegment posX;
    public final MemorySegment posY;
    
    // Configuración del Emisor
    public final MemorySegment emissionRate; // Partículas por segundo
    public final MemorySegment baseVelocity; 
    public final MemorySegment angleSpread;  // Radianes de dispersión

    public DarkParticleEmitterSoA(int capacity) {
        this.capacity = capacity;
        this.arena = Arena.ofShared();
        
        long bytesFloat = capacity * 4L;
        
        this.posX = arena.allocate(bytesFloat);
        this.posY = arena.allocate(bytesFloat);
        this.emissionRate = arena.allocate(bytesFloat);
        this.baseVelocity = arena.allocate(bytesFloat);
        this.angleSpread = arena.allocate(bytesFloat);
        
        // Defaults: Desactivados (Rate = 0)
        for(int i = 0; i < capacity; i++) {
            emissionRate.set(ValueLayout.JAVA_FLOAT, i * 4L, 0.0f);
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void destroy() {
        arena.close();
    }
}
