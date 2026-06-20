// Reading Order: 00100018
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
 * Advanced Post-Processing System (Phase 29).
 * Applies Cinematic Effects (ACES Tone Mapping, Gamma Correction, Bloom Threshold)
 * to the linear HDR Lit texture before FSR upscaling.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader for HDR Post-Processing")
public final class DarkPostProcessSystem {

    private static int computeProgramId;

    public static void init() {
        try {
            DarkLogger.info("GRAPHICS", "Compilando Compute Shader (post_process.comp) en VRAM...");
            
            int shaderId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER);
            
            String source = DarkShaderLoader.loadShader("src/sv/dark/scene/post_process.comp");
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment srcPtr = arena.allocateFrom(source);
                MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            }
            
            DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            computeProgramId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(computeProgramId, shaderId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(computeProgramId);
            
            DarkLogger.info("GRAPHICS", "Post-Process (ACES HDR + Bloom) Compute Shader compilado.");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkPostProcessSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void dispatchPostProcess() {
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(computeProgramId);

            // Bind Lit HDR image (image unit 0) for IN-PLACE Read & Write
            DarkOpenGLLinker.glBindImageTexture.invokeExact(0, DarkDeferredPipeline.getLitTexture(), 0, false, 0, DarkOpenGLLinker.GL_READ_WRITE, DarkOpenGLLinker.GL_RGBA16F);

            // Dispatch 1280x720 in 16x16 work groups
            int groupsX = (DarkDeferredPipeline.INTERNAL_WIDTH + 15) / 16;
            int groupsY = (DarkDeferredPipeline.INTERNAL_HEIGHT + 15) / 16;
            DarkOpenGLLinker.glDispatchCompute.invokeExact(groupsX, groupsY, 1);

            // Synchronize memory before FSR Upscaler
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando Post-Processing", e);
        }
    }
}
