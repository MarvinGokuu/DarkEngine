// Reading Order: 00100019
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.systems.DarkOpenGLLinker;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Data-Oriented Light System (Phase 35+ AZDO).
 * Manages the SSBO containing all Point/Spot lights in the scene.
 * Uses AZDO (Persistent Mapped Buffers) for zero-latency, zero-syscall light updates.
 */
@AAACertified(date = "2026-06-28", maxLatencyNs = 0, minThroughput = 0, lockFree = true, offHeap = true, notes = "SSBO Light Manager - AZDO Persistent Mapping")
public final class DarkLightSystem {

    public static final int MAX_LIGHTS = 1024;
    private static final int LIGHT_STRUCT_BYTES = 32; // vec4 position(x,y,z,radius) + vec4 color(r,g,b,intensity)

    private static int ssboLights;
    private static MemorySegment lightsMemory;
    private static int activeLightCount = 0;
    
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try {
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment bufferPtr = tempArena.allocate(ValueLayout.JAVA_INT);
                DarkOpenGLLinker.glGenBuffers.invokeExact(1, bufferPtr);
                ssboLights = bufferPtr.get(ValueLayout.JAVA_INT, 0);
                
                long size = (long) MAX_LIGHTS * LIGHT_STRUCT_BYTES;
                int flags = DarkOpenGLLinker.GL_MAP_WRITE_BIT | DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
                
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboLights);
                DarkOpenGLLinker.glBufferStorage.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, size, MemorySegment.NULL, flags);
                
                lightsMemory = (MemorySegment) DarkOpenGLLinker.glMapBufferRange.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, size, flags);
                lightsMemory = lightsMemory.reinterpret(size); // Cast to sized segment for direct Java writes
            }
            
            DarkLogger.info("GRAPHICS", "Light System AZDO SSBO initialized (Capacity: " + MAX_LIGHTS + ").");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to init DarkLightSystem", e);
        }
    }

    public static void addPointLight(float x, float y, float z, float radius, float r, float g, float b, float intensity) {
        if (activeLightCount >= MAX_LIGHTS || !isInitialized) return;
        
        long offset = (long) activeLightCount * LIGHT_STRUCT_BYTES;
        // Direct writes into GPU-mapped memory (0 OS syscalls)
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset, x);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 4, y);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 8, z);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 12, radius);
        
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 16, r);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 20, g);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 24, b);
        lightsMemory.set(ValueLayout.JAVA_FLOAT, offset + 28, intensity);
        
        activeLightCount++;
    }
    
    public static void clearLights() {
        activeLightCount = 0;
    }

    /** 
     * AZDO: Since the buffer is mapped with GL_MAP_COHERENT_BIT,
     * memory is automatically synced. This is now a zero-overhead No-Op.
     */
    public static void syncToGPU() {
        // AZDO: No-op. The data is already in VRAM-mapped coherent memory.
        // The Compute Shader execution barrier is handled in DarkClusteredSystem.
    }

    public static int getLightsSSBO() {
        return ssboLights;
    }
    
    public static int getActiveLightCount() {
        return activeLightCount;
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboLights);
            DarkOpenGLLinker.glUnmapBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment bufferPtr = tempArena.allocate(ValueLayout.JAVA_INT);
                bufferPtr.set(ValueLayout.JAVA_INT, 0, ssboLights);
                DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, bufferPtr);
            }
            lightsMemory = null;
            activeLightCount = 0;
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error destroying Light System AZDO");
        }
    }
}
