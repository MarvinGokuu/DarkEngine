// Reading Order: 00100031
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import java.util.Arrays;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;
import sv.dark.scene.DarkTransformSoA;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import java.nio.ByteOrder;

/**
 * Spatial Hash Grid for Broadphase Culling (Data-Oriented).
 * 
 * Particiona el mundo en una cuadrícula (Grid) y agrupa las entidades usando
 * LinkedLists implementadas 100% sobre arreglos primitivos (`int[]`).
 * - 0 Objeto Asignados (Zero-Allocation).
 * - Complejidad de Construcción: O(N).
 * - Complejidad de Búsqueda de Vecinos: O(1) ~ O(K).
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 10, minThroughput = 0, lockFree = true, offHeap = false, notes = "Flat Array Spatial Hashing")
public final class SpatialHashGrid {

    private final int[] cellHead;
    private final int[] cellNext;
    
    private final float cellSize;
    private final int gridWidth;
    private final int gridHeight;
    private final int numCells;

    public SpatialHashGrid(int maxEntities, float cellSize, int gridWidth, int gridHeight) {
        this.cellSize = cellSize;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.numCells = gridWidth * gridHeight;
        
        this.cellHead = new int[numCells];
        this.cellNext = new int[maxEntities];
    }

    /**
     * Limpia la cuadrícula para el siguiente frame O(M).
     */
    public void clear() {
        Arrays.fill(cellHead, -1);
        Arrays.fill(cellNext, -1);
    }

    /**
     * Obtiene el índice 1D de la celda a partir de coordenadas espaciales.
     */
    public int getCellId(double posX, double posY) {
        // Asumiendo que el mundo empieza en (0,0) o aplicamos offset.
        // Si hay coordenadas negativas, ajustamos desplazando el mundo (offset).
        int cx = (int) (posX / cellSize);
        int cy = (int) (posY / cellSize);
        
        // Clamp to edges
        if (cx < 0) cx = 0;
        if (cy < 0) cy = 0;
        if (cx >= gridWidth) cx = gridWidth - 1;
        if (cy >= gridHeight) cy = gridHeight - 1;
        
        return cy * gridWidth + cx;
    }

    public void buildGrid(DarkTransformSoA soa, int maxEntities) {
        clear();
        
        VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
        int upperBound = SPECIES.loopBound(maxEntities);
        
        // Vectorized SIMD loop (8 entities per instruction on AVX-512)
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            DoubleVector xVec = DoubleVector.fromMemorySegment(SPECIES, soa.globalPosX, i * 8L, ByteOrder.nativeOrder());
            VectorMask<Double> activeMask = xVec.compare(VectorOperators.NE, Double.MAX_VALUE);
            
            if (activeMask.anyTrue()) {
                long bits = activeMask.toLong();
                for (int j = 0; j < SPECIES.length(); j++) {
                    if ((bits & (1L << j)) != 0) {
                        int entityId = i + j;
                        double posX = xVec.lane(j);
                        double posY = soa.globalPosY.get(ValueLayout.JAVA_DOUBLE, entityId * 8L);
                        
                        int cellId = getCellId(posX, posY);
                        
                        // Insertar al inicio de la lista enlazada simulada
                        cellNext[entityId] = cellHead[cellId];
                        cellHead[cellId] = entityId;
                    }
                }
            }
        }
        
        // Tail loop for remaining entities
        for (int entityId = upperBound; entityId < maxEntities; entityId++) {
            double posX = soa.globalPosX.get(ValueLayout.JAVA_DOUBLE, entityId * 8L);
            if (posX == Double.MAX_VALUE) continue;
            
            double posY = soa.globalPosY.get(ValueLayout.JAVA_DOUBLE, entityId * 8L);
            int cellId = getCellId(posX, posY);
            
            // Insertar al inicio de la lista enlazada simulada
            cellNext[entityId] = cellHead[cellId];
            cellHead[cellId] = entityId;
        }
    }

    public int getHeadEntity(int cellId) {
        if (cellId < 0 || cellId >= numCells) return -1;
        return cellHead[cellId];
    }

    public int getNextEntity(int entityId) {
        if (entityId < 0 || entityId >= cellNext.length) return -1;
        return cellNext[entityId];
    }

    public void destroy() {
        // Ayudar al GC durante el Graceful Shutdown
        Arrays.fill(cellHead, -1);
        Arrays.fill(cellNext, -1);
    }
}
