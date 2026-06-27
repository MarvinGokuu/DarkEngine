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
 * Data-Oriented Light System (Phase 29 - Clustered Deferred).
 * Manages the SSBO containing all Point/Spot lights in the scene.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "SSBO Light Manager for Compute Shaders")
public final class DarkLightSystem {

    public static final int MAX_LIGHTS = 1024;
    private static final int LIGHT_STRUCT_BYTES = 32; // vec4 position(x,y,z,radius) + vec4 color(r,g,b,intensity)

    private static int ssboLights;
    private static MemorySegment lightsMemory;
    private static final Arena lightArena = Arena.ofAuto();
    private static int activeLightCount = 0;
    
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try {
            // Allocate Off-Heap memory for lights
            lightsMemory = lightArena.allocate(MAX_LIGHTS * LIGHT_STRUCT_BYTES);
            
            try (Arena tempArena = Arena.ofConfined()) {
                MemorySegment bufferPtr = tempArena.allocate(ValueLayout.JAVA_INT);
                DarkOpenGLLinker.glGenBuffers.invokeExact(1, bufferPtr);
                ssboLights = bufferPtr.get(ValueLayout.JAVA_INT, 0);
                
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboLights);
                DarkOpenGLLinker.glBufferData.invokeExact(
                    DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 
                    (long)(MAX_LIGHTS * LIGHT_STRUCT_BYTES), 
                    MemorySegment.NULL, 
                    DarkOpenGLLinker.GL_DYNAMIC_DRAW
                );
            }
            
            DarkLogger.info("GRAPHICS", "Light System SSBO initialized (Capacity: " + MAX_LIGHTS + ").");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to init DarkLightSystem", e);
        }
    }

    public static void addPointLight(float x, float y, float z, float radius, float r, float g, float b, float intensity) {
        if (activeLightCount >= MAX_LIGHTS) return;
        
        long offset = (long) activeLightCount * LIGHT_STRUCT_BYTES;
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

    /** Call every frame to sync CPU off-heap memory to VRAM */
    public static void syncToGPU() {
        if (!isInitialized || activeLightCount == 0) return;
        try {
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboLights);
            DarkOpenGLLinker.glBufferSubData.invokeExact(
                DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 
                0L, 
                (long)(activeLightCount * LIGHT_STRUCT_BYTES), 
                lightsMemory
            );
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error syncing lights to GPU");
        }
    }

    public static int getLightsSSBO() {
        return ssboLights;
    }
    
    public static int getActiveLightCount() {
        return activeLightCount;
    }

    public static void destroy() {
        if (!isInitialized) return;
        try (Arena tempArena = Arena.ofConfined()) {
            MemorySegment bufferPtr = tempArena.allocate(ValueLayout.JAVA_INT);
            bufferPtr.set(ValueLayout.JAVA_INT, 0, ssboLights);
            DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, bufferPtr);
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error destroying Light System");
        }
    }
}
