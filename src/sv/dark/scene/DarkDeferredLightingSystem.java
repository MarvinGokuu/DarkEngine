// Reading Order: 00100016
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.systems.DarkOpenGLLinker;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout;
import sv.dark.scene.DarkShaderLoader;

/**
 * Deferred Lighting System (Phase 27).
 * Reads the G-Buffer and applies lighting via Compute Shader.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader for Deferred Lighting")
public final class DarkDeferredLightingSystem {

    private static int computeProgramId;
    private static int sunDirLocation;
    private static int sunColorLocation;
    private static int camPosLocation;

    public static void init() {
        try {
            DarkLogger.info("GRAPHICS", "Compilando Compute Shader (deferred_lighting.comp) en VRAM...");
            
            int shaderId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER);
            
            String source = DarkShaderLoader.loadShader("src/sv/dark/scene/deferred_lighting.comp");
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment srcPtr = arena.allocateFrom(source);
                MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            }
            
            DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            computeProgramId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(computeProgramId, shaderId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(computeProgramId);
            
            try (Arena arenaLocal = Arena.ofConfined()) {
                MemorySegment nameDir = arenaLocal.allocateFrom("sunDir");
                sunDirLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, nameDir);
                
                MemorySegment nameColor = arenaLocal.allocateFrom("sunColor");
                sunColorLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, nameColor);

                MemorySegment nameCamPos = arenaLocal.allocateFrom("camPos");
                camPosLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, nameCamPos);
            }
            
            DarkLogger.info("GRAPHICS", "Deferred Lighting Compute Shader compilado.");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkDeferredLightingSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void dispatchLighting() {
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(computeProgramId);

            // Dynamic sun variables (future: link to ECS)
            float sunX = (float) Math.sin(System.currentTimeMillis() * 0.001) * 0.5f;
            float sunY = 1.0f;
            float sunZ = (float) Math.cos(System.currentTimeMillis() * 0.001) * 0.5f;
            
            // Normalize
            float length = (float) Math.sqrt(sunX*sunX + sunY*sunY + sunZ*sunZ);
            sunX /= length; sunY /= length; sunZ /= length;
            
            DarkOpenGLLinker.glUniform3f.invokeExact(sunDirLocation, sunX, sunY, sunZ);
            DarkOpenGLLinker.glUniform3f.invokeExact(sunColorLocation, 1.5f, 1.425f, 1.2f);
            DarkOpenGLLinker.glUniform3f.invokeExact(camPosLocation, 0.0f, 5.0f, 10.0f); // TODO: Ligar ECS Camera Position

            // Bind Albedo (texture unit 0)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE0);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getAlbedoTexture());
            
            // Bind Normal (texture unit 1)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE1);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getNormalTexture());

            // Bind PBR (texture unit 2)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE2);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getPbrTexture());

            // Bind Lit output image (image unit 3)
            DarkOpenGLLinker.glBindImageTexture.invokeExact(3, DarkDeferredPipeline.getLitTexture(), 0, false, 0, DarkOpenGLLinker.GL_READ_WRITE, DarkOpenGLLinker.GL_RGBA16F);

            // Dispatch 1280x720 in 16x16 work groups
            int groupsX = (DarkDeferredPipeline.INTERNAL_WIDTH + 15) / 16;
            int groupsY = (DarkDeferredPipeline.INTERNAL_HEIGHT + 15) / 16;
            DarkOpenGLLinker.glDispatchCompute.invokeExact(groupsX, groupsY, 1);

            // Synchronize memory before FSR reads it
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando Deferred Lighting", e);
        }
    }
}
