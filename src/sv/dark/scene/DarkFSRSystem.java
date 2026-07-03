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
            DarkLogger.info("GRAPHICS", "Compilando Compute Shader (fsr_upscale.comp) en VRAM vía RHI...");
            
            sv.dark.rhi.DarkRHI rhi = sv.dark.core.DarkRHIContext.get();
            String source = DarkShaderLoader.loadShader("src/sv/dark/scene/fsr_upscale.comp");
            computeProgramId = rhi.createComputeShader(source);
            
            DarkLogger.info("GRAPHICS", "FSR Upscale Compute Shader compilado. Target: 4K");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkFSRSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void dispatchFSR() {
        try {
            sv.dark.rhi.DarkRHI rhi = sv.dark.core.DarkRHIContext.get();
            rhi.useProgram(computeProgramId);

            // Bind Lit texture (texture unit 0)
            rhi.bindTexture2D(DarkDeferredPipeline.getLitTexture());

            // Bind Presentation 4K output image (image unit 1)
            rhi.bindImageTexture(1, DarkDeferredPipeline.getPresentationTexture(), 0, false, 0, sv.dark.rhi.DarkRHI.ACCESS_READ_WRITE, sv.dark.rhi.DarkRHI.FORMAT_RGBA8);

            // Dispatch 3840x2160 in 16x16 work groups
            int groupsX = (sv.dark.config.DarkDisplayConfig.targetWidth + 15) / 16;
            int groupsY = (sv.dark.config.DarkDisplayConfig.targetHeight + 15) / 16;
            rhi.dispatchCompute(groupsX, groupsY, 1);

            // Synchronize memory before Window swap
            rhi.memoryBarrier(sv.dark.rhi.DarkRHI.BARRIER_SHADER_IMAGE);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando FSR Upscale", e);
        }
    }

    public static void destroy() {
        try {
            sv.dark.rhi.DarkRHI rhi = sv.dark.core.DarkRHIContext.get();
            if (rhi != null && computeProgramId != 0) {
                rhi.deleteProgram(computeProgramId);
                computeProgramId = 0;
            }
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error al destruir shader de FSR Upscale", e);
        }
    }
}
