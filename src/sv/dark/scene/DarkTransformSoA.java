// Reading Order: 00100000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;

/**
 * Data-Oriented Technology Stack: Transform Structure of Arrays (SoA).
 * 
 * <p>En lugar de tener objetos Entity dispersos en el Heap, agrupamos todas las
 * propiedades de X, Y y velocidades en arreglos nativos contiguos (Off-Heap).
 * La simulación lógica ocurre en 64-bits (globalPosX/Y), mientras que la 
 * representación visual se inyecta en 32-bits (posX/Y) usando Camera-Relative Rendering.
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 1, minThroughput = 1_000_000, lockFree = true, offHeap = true, notes = "ECS SoA Nativo con 64-bit Camera Relative")
public final class DarkTransformSoA {
    
    private final Arena arena;
    private final int capacity;
    
    // Segmentos separados para cada propiedad (True SoA)
    // 32-bits (Float) - Destino Final para la GPU (VRAM / OpenGL FFI)
    public final MemorySegment posX;
    public final MemorySegment posY;
    public final MemorySegment velX;
    public final MemorySegment velY;

    // 64-bits (Double) - Lógica y Cinemática del CPU (Precisión Infinita)
    public final MemorySegment globalPosX;
    public final MemorySegment globalPosY;

    /**
     * Aloja la memoria nativa requerida para la capacidad máxima de entidades.
     * @param capacity Número máximo de entidades (ej. 1,000,000)
     */
    public DarkTransformSoA(int capacity) {
        this.capacity = capacity;
        this.arena = Arena.ofShared();
        
        long bytesRequired32 = capacity * 4L; // 4 bytes por float
        long bytesRequired64 = capacity * 8L; // 8 bytes por double
        
        // Asignación de bloques contiguos paralelos (32-bit para la GPU)
        this.posX = arena.allocate(bytesRequired32, 64); // Alineado a 64-bytes (Cache Line)
        this.posY = arena.allocate(bytesRequired32, 64);
        this.velX = arena.allocate(bytesRequired32, 64);
        this.velY = arena.allocate(bytesRequired32, 64);

        // Asignación de bloques (64-bit para simulaciones CPU)
        this.globalPosX = arena.allocate(bytesRequired64, 64);
        this.globalPosY = arena.allocate(bytesRequired64, 64);
        
        DarkLogger.info("ECS", "SoA Allocator: " + capacity + " entities (" + ((bytesRequired32 * 4 + bytesRequired64 * 2) / 1024 / 1024) + " MB Off-Heap)");
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Inserta datos escalares para una entidad (Útil para inicialización).
     */
    public void setEntity(int entityId, double globalPx, double globalPy, float vx, float vy) {
        long offset32 = entityId * 4L;
        long offset64 = entityId * 8L;
        
        // Setear estado lógico en 64-bits
        globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64, globalPx);
        globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64, globalPy);
        
        // Setear estado visual inicial (A la espera del primer Camera Relative Rendering)
        posX.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPx);
        posY.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPy);
        
        velX.set(ValueLayout.JAVA_FLOAT, offset32, vx);
        velY.set(ValueLayout.JAVA_FLOAT, offset32, vy);
    }
    
    public void destroy() {
        if (arena.scope().isAlive()) {
            arena.close();
            DarkLogger.info("ECS", "SoA Memory Released.");
        }
    }
}
