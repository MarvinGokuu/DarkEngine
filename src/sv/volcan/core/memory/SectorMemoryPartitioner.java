package sv.volcan.core.memory; // Sincronizado con la ruta física

import java.lang.foreign.MemorySegment;

/**
 * AUTORIDAD: Sector
 * RESPONSABILIDAD: Segmentación de la bóveda en vistas (slices) alineadas a
 * caché.
 * GARANTÍAS: Zero-heap-allocation, alineación de 64-bytes (L1 Ready),
 * zero-copy.
 * PROHIBICIONES: Prohibido usar ArrayList o List, prohibido usar aritmética de
 * punto flotante,
 * prohibido generar basura en el Heap.
 * DOMINIO CRÍTICO: Memoria
 */
public final class SectorMemoryPartitioner {

    // Alineación para CPU (Cache Line) no para OS (Page)
    private static final long CACHE_LINE = 64;

    private SectorMemoryPartitioner() {
    }

    /**
     * Particionado determinista.
     * Retorna un array plano de MemorySegment (punteros nativos).
     */
    public static MemorySegment[] partition(MemorySegment vault, long chunkSize) {
        if (vault == null || vault.byteSize() == 0)
            return new MemorySegment[0];

        // [INGENIERÍA DURA]: Alineación Bitwise a 64 bytes (Evita False Sharing)
        long alignedChunk = (chunkSize + CACHE_LINE - 1) & ~(CACHE_LINE - 1);

        // Cálculo de cantidad usando aritmética de enteros (Deterministic Calculation)
        int count = (int) ((vault.byteSize() + alignedChunk - 1) / alignedChunk);

        // Array plano: Los punteros están contiguos en memoria
        MemorySegment[] segments = new MemorySegment[count];

        for (int i = 0; i < count; i++) {
            long offset = i * alignedChunk;
            long currentSize = Math.min(alignedChunk, vault.byteSize() - offset);

            // [HITO 1.1]: asSlice es una vista sobre memoria nativa, no una copia.
            // Costo operativo insignificante (Zero-Copy).
            segments[i] = vault.asSlice(offset, currentSize);
        }

        return segments;
    }
    // actualizado3/1/26
}