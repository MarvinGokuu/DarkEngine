// Reading Order: 00100050
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.vfx.animation;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * CPU-Side Skeleton configurations.
 * Almacena las matrices de transformacion (4x4) de los huesos para todos los esqueletos
 * del mundo, aplanadas en un solo bloque contiguo de memoria Off-Heap.
 */
@AAACertified(date = "2026-06-20", maxLatencyNs = 10, minThroughput = 0, lockFree = true, offHeap = true, notes = "Phase 32.2: Contiguous Matrix Block for GPU Upload")
public final class DarkSkeletonSoA {

    private final Arena arena;
    private final int capacity; // Number of skeletons
    private final int maxBonesPerSkeleton;

    // Matriz 4x4 = 16 floats = 64 bytes.
    // Segmento Unico Contiguo para Subida Masiva a OpenGL (glBufferSubData)
    private final MemorySegment boneMatrices;

    public DarkSkeletonSoA(int capacity, int maxBonesPerSkeleton) {
        this.capacity = capacity;
        this.maxBonesPerSkeleton = maxBonesPerSkeleton;
        this.arena = Arena.ofShared();
        
        long totalBytes = (long) capacity * maxBonesPerSkeleton * 16L * 4L;
        this.boneMatrices = arena.allocate(totalBytes);
        
        // Cargar Matrices Identidad por defecto
        for (int i = 0; i < capacity * maxBonesPerSkeleton; i++) {
            setIdentity(i);
        }
    }

    private void setIdentity(int boneGlobalIndex) {
        long offset = boneGlobalIndex * 16L * 4L;
        
        // Limpiar a 0
        for(int j=0; j<16; j++) {
            boneMatrices.set(ValueLayout.JAVA_FLOAT, offset + (j * 4L), 0.0f);
        }
        
        // Diagonal a 1.0 (m00, m11, m22, m33)
        boneMatrices.set(ValueLayout.JAVA_FLOAT, offset + (0 * 4L), 1.0f);
        boneMatrices.set(ValueLayout.JAVA_FLOAT, offset + (5 * 4L), 1.0f);
        boneMatrices.set(ValueLayout.JAVA_FLOAT, offset + (10 * 4L), 1.0f);
        boneMatrices.set(ValueLayout.JAVA_FLOAT, offset + (15 * 4L), 1.0f);
    }

    /**
     * Obtiene el bloque de memoria contigua listo para ser transferido a VRAM
     * sin deserialización, usando glBufferSubData().
     */
    public MemorySegment getRawBuffer() {
        return boneMatrices;
    }

    public long getSizeBytes() {
        return (long) capacity * maxBonesPerSkeleton * 16L * 4L;
    }

    public int getCapacity() {
        return capacity;
    }

    public void destroy() {
        arena.close();
    }
}
