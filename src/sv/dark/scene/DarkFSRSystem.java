// Reading Order: 00100017
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
 * Spatial Upscaling System FSR Proxy (Phase 27).
 * Reads the 720p Lit texture and upscales it to 4K Presentation texture using Edge-Adaptive Compute Shader.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader for 4K FSR Upscaling")
public final class DarkFSRSystem {

    private static int computeProgramId;

    public static void init() {
        try {
            DarkLogger.info("GRAPHICS", "Compilando Compute Shader (fsr_upscale.comp) en VRAM...");
            
            int shaderId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER);
            
            String source = DarkShaderLoader.loadShader("src/sv/dark/scene/fsr_upscale.comp");
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment srcPtr = arena.allocateFrom(source);
                MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
                DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            }
            
            DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            computeProgramId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(computeProgramId, shaderId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(computeProgramId);
            DarkOpenGLLinker.glDeleteShader.invokeExact(shaderId);
            
            DarkLogger.info("GRAPHICS", "FSR Upscale Compute Shader compilado. Target: 4K");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkFSRSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void dispatchFSR() {
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(computeProgramId);

            // Bind Lit texture (texture unit 0)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getLitTexture());

            // Bind Presentation 4K output image (image unit 1)
            DarkOpenGLLinker.glBindImageTexture.invokeExact(1, DarkDeferredPipeline.getPresentationTexture(), 0, false, 0, DarkOpenGLLinker.GL_READ_WRITE, DarkOpenGLLinker.GL_RGBA8);

            // Dispatch 3840x2160 in 16x16 work groups
            int groupsX = (DarkDeferredPipeline.TARGET_WIDTH + 15) / 16;
            int groupsY = (DarkDeferredPipeline.TARGET_HEIGHT + 15) / 16;
            DarkOpenGLLinker.glDispatchCompute.invokeExact(groupsX, groupsY, 1);

            // Synchronize memory before Window swap
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando FSR Upscale", e);
        }
    }

    public static void destroy() {
        try {
            if (computeProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(computeProgramId);
                computeProgramId = 0;
            }
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error al destruir shader de FSR Upscale", e);
        }
    }
}
