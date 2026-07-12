// Reading Order: 01001000
//  72
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
    public final MemorySegment posZ;
    public final MemorySegment velX;
    public final MemorySegment velY;
    public final MemorySegment velZ;

    // 64-bits (Double) - Lógica y Cinemática del CPU (Precisión Infinita)
    public final MemorySegment localPosX;
    public final MemorySegment localPosY;
    public final MemorySegment localPosZ;
    
    public final MemorySegment globalPosX;
    public final MemorySegment globalPosY;
    public final MemorySegment globalPosZ;

    // Jerarquía de Escena (Scene Graph) - Índices Físicos (32-bits)
    public final MemorySegment parentIdx;

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
        this.posZ = arena.allocate(bytesRequired32, 64);
        this.velX = arena.allocate(bytesRequired32, 64);
        this.velY = arena.allocate(bytesRequired32, 64);
        this.velZ = arena.allocate(bytesRequired32, 64);

        // Asignación de bloques (64-bit para simulaciones CPU)
        this.localPosX = arena.allocate(bytesRequired64, 64);
        this.localPosY = arena.allocate(bytesRequired64, 64);
        this.localPosZ = arena.allocate(bytesRequired64, 64);
        
        this.globalPosX = arena.allocate(bytesRequired64, 64);
        this.globalPosY = arena.allocate(bytesRequired64, 64);
        this.globalPosZ = arena.allocate(bytesRequired64, 64);
        
        // Asignación de bloques (Jerarquía de Escena)
        this.parentIdx = arena.allocate(bytesRequired32, 64);
        
        // Inicializar jerarquía con -1 (Sin padre) usando relleno de bytes rápido (0xFFFFFFFF = -1 en int de 32-bits)
        parentIdx.fill((byte) 0xFF);
        
        DarkLogger.info("ECS", "SoA Allocator: " + capacity + " entities (" + ((bytesRequired32 * 9 + bytesRequired64 * 3) / 1024 / 1024) + " MB Off-Heap 3D LWC)");
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Inserta datos escalares para una entidad (Útil para inicialización).
     */
    public void setEntity(int entityId, double globalPx, double globalPy, double globalPz, float vx, float vy, float vz) {
        long offset32 = entityId * 4L;
        long offset64 = entityId * 8L;
        
        // Setear estado lógico en 64-bits
        localPosX.set(ValueLayout.JAVA_DOUBLE, offset64, globalPx);
        localPosY.set(ValueLayout.JAVA_DOUBLE, offset64, globalPy);
        localPosZ.set(ValueLayout.JAVA_DOUBLE, offset64, globalPz);
        
        globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64, globalPx);
        globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64, globalPy);
        globalPosZ.set(ValueLayout.JAVA_DOUBLE, offset64, globalPz);
        
        // Setear estado visual inicial (A la espera del primer Camera Relative Rendering)
        posX.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPx);
        posY.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPy);
        posZ.set(ValueLayout.JAVA_FLOAT, offset32, (float) globalPz);
        
        velX.set(ValueLayout.JAVA_FLOAT, offset32, vx);
        velY.set(ValueLayout.JAVA_FLOAT, offset32, vy);
        velZ.set(ValueLayout.JAVA_FLOAT, offset32, vz);
        
        parentIdx.set(ValueLayout.JAVA_INT, offset32, -1);
    }
    
    /**
     * Intercambia físicamente dos entidades en la memoria O(1).
     * Crítico para el Topological Sort.
     */
    public void swap(int idA, int idB) {
        if (idA == idB) return;
        long offA32 = idA * 4L;
        long offB32 = idB * 4L;
        long offA64 = idA * 8L;
        long offB64 = idB * 8L;

        // Swap 64-bit
        double tempD;
        tempD = localPosX.get(ValueLayout.JAVA_DOUBLE, offA64);
        localPosX.set(ValueLayout.JAVA_DOUBLE, offA64, localPosX.get(ValueLayout.JAVA_DOUBLE, offB64));
        localPosX.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);
        
        tempD = localPosY.get(ValueLayout.JAVA_DOUBLE, offA64);
        localPosY.set(ValueLayout.JAVA_DOUBLE, offA64, localPosY.get(ValueLayout.JAVA_DOUBLE, offB64));
        localPosY.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);
        
        tempD = localPosZ.get(ValueLayout.JAVA_DOUBLE, offA64);
        localPosZ.set(ValueLayout.JAVA_DOUBLE, offA64, localPosZ.get(ValueLayout.JAVA_DOUBLE, offB64));
        localPosZ.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);
        
        tempD = globalPosX.get(ValueLayout.JAVA_DOUBLE, offA64);
        globalPosX.set(ValueLayout.JAVA_DOUBLE, offA64, globalPosX.get(ValueLayout.JAVA_DOUBLE, offB64));
        globalPosX.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);
        
        tempD = globalPosY.get(ValueLayout.JAVA_DOUBLE, offA64);
        globalPosY.set(ValueLayout.JAVA_DOUBLE, offA64, globalPosY.get(ValueLayout.JAVA_DOUBLE, offB64));
        globalPosY.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);
        
        tempD = globalPosZ.get(ValueLayout.JAVA_DOUBLE, offA64);
        globalPosZ.set(ValueLayout.JAVA_DOUBLE, offA64, globalPosZ.get(ValueLayout.JAVA_DOUBLE, offB64));
        globalPosZ.set(ValueLayout.JAVA_DOUBLE, offB64, tempD);

        // Swap 32-bit (Floats)
        float tempF;
        tempF = posX.get(ValueLayout.JAVA_FLOAT, offA32);
        posX.set(ValueLayout.JAVA_FLOAT, offA32, posX.get(ValueLayout.JAVA_FLOAT, offB32));
        posX.set(ValueLayout.JAVA_FLOAT, offB32, tempF);
        
        tempF = posY.get(ValueLayout.JAVA_FLOAT, offA32);
        posY.set(ValueLayout.JAVA_FLOAT, offA32, posY.get(ValueLayout.JAVA_FLOAT, offB32));
        posY.set(ValueLayout.JAVA_FLOAT, offB32, tempF);
        
        tempF = posZ.get(ValueLayout.JAVA_FLOAT, offA32);
        posZ.set(ValueLayout.JAVA_FLOAT, offA32, posZ.get(ValueLayout.JAVA_FLOAT, offB32));
        posZ.set(ValueLayout.JAVA_FLOAT, offB32, tempF);
        
        tempF = velX.get(ValueLayout.JAVA_FLOAT, offA32);
        velX.set(ValueLayout.JAVA_FLOAT, offA32, velX.get(ValueLayout.JAVA_FLOAT, offB32));
        velX.set(ValueLayout.JAVA_FLOAT, offB32, tempF);
        
        tempF = velY.get(ValueLayout.JAVA_FLOAT, offA32);
        velY.set(ValueLayout.JAVA_FLOAT, offA32, velY.get(ValueLayout.JAVA_FLOAT, offB32));
        velY.set(ValueLayout.JAVA_FLOAT, offB32, tempF);
        
        tempF = velZ.get(ValueLayout.JAVA_FLOAT, offA32);
        velZ.set(ValueLayout.JAVA_FLOAT, offA32, velZ.get(ValueLayout.JAVA_FLOAT, offB32));
        velZ.set(ValueLayout.JAVA_FLOAT, offB32, tempF);

        // Swap 32-bit (Ints - Hierarchy)
        int tempI;
        tempI = parentIdx.get(ValueLayout.JAVA_INT, offA32);
        parentIdx.set(ValueLayout.JAVA_INT, offA32, parentIdx.get(ValueLayout.JAVA_INT, offB32));
        parentIdx.set(ValueLayout.JAVA_INT, offB32, tempI);
    }
    
    public void destroy() {
        if (arena.scope().isAlive()) {
            arena.close();
            DarkLogger.info("ECS", "SoA Memory Released.");
        }
    }
}
