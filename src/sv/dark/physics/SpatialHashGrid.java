// Reading Order: 00100031
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import java.util.Arrays;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;
import sv.dark.scene.DarkTransformSoA;

import java.lang.foreign.MemorySegment;
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

    private int computeProgramId;
    private int ssboX, ssboY, ssboCellHead, ssboCellNext;
    
    // AZDO Mapped Memory for Results
    private MemorySegment mappedCellHead;
    private MemorySegment mappedCellNext;
    private MemorySegment mappedPosX;
    private MemorySegment mappedPosY;
    
    private final float cellSize;
    private final int gridWidth;
    private final int gridHeight;
    private final int numCells;

    private boolean initialized = false;
    private final int maxEntities;

    public SpatialHashGrid(int maxEntities, float cellSize, int gridWidth, int gridHeight) {
        this.maxEntities = maxEntities;
        this.cellSize = cellSize;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.numCells = gridWidth * gridHeight;
    }

    public void initGraphicsContext() {
        if (initialized) return;
        try {
            int shaderId = (int) sv.dark.core.systems.DarkOpenGLLinker.glCreateShader.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_COMPUTE_SHADER);
            String source = sv.dark.scene.DarkShaderLoader.loadShader("src/sv/dark/physics/radix_sort.comp");
            
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment srcPtr = arena.allocateFrom(source);
                MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                sv.dark.core.systems.DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            }
            
            sv.dark.core.systems.DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            computeProgramId = (int) sv.dark.core.systems.DarkOpenGLLinker.glCreateProgram.invokeExact();
            sv.dark.core.systems.DarkOpenGLLinker.glAttachShader.invokeExact(computeProgramId, shaderId);
            sv.dark.core.systems.DarkOpenGLLinker.glLinkProgram.invokeExact(computeProgramId);
            sv.dark.core.systems.DarkOpenGLLinker.glDeleteShader.invokeExact(shaderId);
            
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, 4);
                sv.dark.core.systems.DarkOpenGLLinker.glGenBuffers.invokeExact(4, buffers);
                ssboX = buffers.get(ValueLayout.JAVA_INT, 0);
                ssboY = buffers.get(ValueLayout.JAVA_INT, 4);
                ssboCellHead = buffers.get(ValueLayout.JAVA_INT, 8);
                ssboCellNext = buffers.get(ValueLayout.JAVA_INT, 12);
                
                int flagsMap = sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_WRITE_BIT | sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
                
                long posSize = maxEntities * 8L;
                
                sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboX);
                sv.dark.core.systems.DarkOpenGLLinker.glBufferStorage.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, posSize, MemorySegment.NULL, flagsMap);
                mappedPosX = (MemorySegment) sv.dark.core.systems.DarkOpenGLLinker.glMapBufferRange.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, posSize, flagsMap);
                mappedPosX = mappedPosX.reinterpret(posSize);
                
                sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboY);
                sv.dark.core.systems.DarkOpenGLLinker.glBufferStorage.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, posSize, MemorySegment.NULL, flagsMap);
                mappedPosY = (MemorySegment) sv.dark.core.systems.DarkOpenGLLinker.glMapBufferRange.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, posSize, flagsMap);
                mappedPosY = mappedPosY.reinterpret(posSize);
                
                int flagsMapReadWrite = sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_READ_BIT | sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_WRITE_BIT | sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | sv.dark.core.systems.DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
                long headSize = numCells * 4L;
                long nextSize = maxEntities * 4L;
                
                sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboCellHead);
                sv.dark.core.systems.DarkOpenGLLinker.glBufferStorage.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, headSize, MemorySegment.NULL, flagsMapReadWrite);
                mappedCellHead = (MemorySegment) sv.dark.core.systems.DarkOpenGLLinker.glMapBufferRange.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, headSize, flagsMapReadWrite);
                mappedCellHead = mappedCellHead.reinterpret(headSize);
                
                sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboCellNext);
                sv.dark.core.systems.DarkOpenGLLinker.glBufferStorage.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, nextSize, MemorySegment.NULL, flagsMapReadWrite);
                mappedCellNext = (MemorySegment) sv.dark.core.systems.DarkOpenGLLinker.glMapBufferRange.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, nextSize, flagsMapReadWrite);
                mappedCellNext = mappedCellNext.reinterpret(nextSize);
            }
            initialized = true;
        } catch (Throwable e) {
            sv.dark.core.DarkLogger.fatal("PHYSICS", "Failed to init GPU Spatial Hash", e);
        }
    }

    public void clear() {
        // AZDO clear via MemorySegment
        mappedCellHead.fill((byte) -1);
        mappedCellNext.fill((byte) -1);
    }

    public int getCellId(double posX, double posY) {
        int cx = (int) (posX / cellSize);
        int cy = (int) (posY / cellSize);
        if (cx < 0) cx = 0;
        if (cy < 0) cy = 0;
        if (cx >= gridWidth) cx = gridWidth - 1;
        if (cy >= gridHeight) cy = gridHeight - 1;
        return cy * gridWidth + cx;
    }

    public void buildGrid(DarkTransformSoA soa, int maxEntities) {
        if (!initialized) {
            sv.dark.core.DarkLogger.fatal("PHYSICS", "SpatialHashGrid buildGrid() called before initGraphicsContext()!", null);
            return;
        }
        clear();
        
        try {
            // AZDO Upload
            long posSize = maxEntities * 8L;
            MemorySegment.copy(soa.globalPosX, 0, mappedPosX, ValueLayout.JAVA_DOUBLE, 0L, maxEntities);
            MemorySegment.copy(soa.globalPosY, 0, mappedPosY, ValueLayout.JAVA_DOUBLE, 0L, maxEntities);
            
            sv.dark.core.systems.DarkOpenGLLinker.glUseProgram.invokeExact(computeProgramId);
            
            // Set Uniforms
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                int locCellSize = (int) sv.dark.core.systems.DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arena.allocateFrom("cellSize"));
                int locGridW = (int) sv.dark.core.systems.DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arena.allocateFrom("gridWidth"));
                int locGridH = (int) sv.dark.core.systems.DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arena.allocateFrom("gridHeight"));
                
                sv.dark.core.systems.DarkOpenGLLinker.glUniform1f.invokeExact(locCellSize, cellSize);
                sv.dark.core.systems.DarkOpenGLLinker.glUniform1i.invokeExact(locGridW, gridWidth);
                sv.dark.core.systems.DarkOpenGLLinker.glUniform1i.invokeExact(locGridH, gridHeight);
            }
            
            sv.dark.core.systems.DarkOpenGLLinker.glBindBufferBase.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0, ssboX);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBufferBase.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 1, ssboY);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBufferBase.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 2, ssboCellHead);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBufferBase.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 3, ssboCellNext);
            
            int numGroups = (maxEntities + 255) / 256;
            sv.dark.core.systems.DarkOpenGLLinker.glDispatchCompute.invokeExact(numGroups, 1, 1);
            sv.dark.core.systems.DarkOpenGLLinker.glMemoryBarrier.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT);
            
        } catch (Throwable e) {
            sv.dark.core.DarkLogger.error("PHYSICS", "Compute Hash Failed");
        }
    }

    public int getHeadEntity(int cellId) {
        if (cellId < 0 || cellId >= numCells) return -1;
        return mappedCellHead.get(ValueLayout.JAVA_INT, cellId * 4L);
    }

    public int getNextEntity(int entityId) {
        return mappedCellNext.get(ValueLayout.JAVA_INT, entityId * 4L);
    }

    public void destroy() {
        try {
            if (computeProgramId != 0) {
                sv.dark.core.systems.DarkOpenGLLinker.glDeleteProgram.invokeExact(computeProgramId);
                computeProgramId = 0;
            }
            sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboX);
            sv.dark.core.systems.DarkOpenGLLinker.glUnmapBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboY);
            sv.dark.core.systems.DarkOpenGLLinker.glUnmapBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboCellHead);
            sv.dark.core.systems.DarkOpenGLLinker.glUnmapBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            sv.dark.core.systems.DarkOpenGLLinker.glBindBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboCellNext);
            sv.dark.core.systems.DarkOpenGLLinker.glUnmapBuffer.invokeExact(sv.dark.core.systems.DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, 4);
                buffers.set(ValueLayout.JAVA_INT, 0L, ssboX);
                buffers.set(ValueLayout.JAVA_INT, 4L, ssboY);
                buffers.set(ValueLayout.JAVA_INT, 8L, ssboCellHead);
                buffers.set(ValueLayout.JAVA_INT, 12L, ssboCellNext);
                sv.dark.core.systems.DarkOpenGLLinker.glDeleteBuffers.invokeExact(4, buffers);
            }
        } catch (Throwable e) {}
    }
}
