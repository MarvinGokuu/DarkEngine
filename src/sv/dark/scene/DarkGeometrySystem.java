// Reading Order: 01110010
//  114
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.rhi.DarkRHI;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Geometry Rendering System (Phase 35+ AZDO).
 * Executes the Geometry Pass: drawing 3D entities into the G-Buffer MRTs.
 * Uses Persistent Mapped Buffers (AZDO) for zero-FFI matrix uploads.
 */
@AAACertified(date = "2026-06-28", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Rasterization Pipeline with AZDO Persistent Buffers")
public final class DarkGeometrySystem {

    private static int shaderProgramId;
    
    // Uniform Locations
    private static int viewLoc;
    private static int projLoc;
    private static int diffuseLoc;
    private static int normalLoc;
    private static int pbrLoc;
    private static int colorMultLoc;

    // AZDO State
    private static int ssboMatricesId;
    private static MemorySegment mappedMatricesPtr;
    private static int instanceCount = 0;
    public static final int MAX_ENTITIES = 100_000;

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try {
            DarkLogger.info("GRAPHICS", "Compiling Geometry Pass Shaders in VRAM...");
            
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            
            String vertSource = DarkShaderLoader.loadShader("src/sv/dark/scene/geometry_pass.vert");
            String fragSource = DarkShaderLoader.loadShader("src/sv/dark/scene/geometry_pass.frag");
            shaderProgramId = device.createGraphicsPipeline(vertSource, fragSource);

            viewLoc = device.getUniformLocation(shaderProgramId, "view");
            projLoc = device.getUniformLocation(shaderProgramId, "projection");
            diffuseLoc = device.getUniformLocation(shaderProgramId, "texture_diffuse1");
            normalLoc = device.getUniformLocation(shaderProgramId, "texture_normal1");
            pbrLoc = device.getUniformLocation(shaderProgramId, "texture_pbr1");
            colorMultLoc = device.getUniformLocation(shaderProgramId, "colorMultiplier");
            
            // AZDO SSBO Initialization
            int flags = sv.dark.rhi.DarkRHI.MAP_WRITE_BIT | sv.dark.rhi.DarkRHI.MAP_PERSISTENT_BIT | sv.dark.rhi.DarkRHI.MAP_COHERENT_BIT;
            long size = (long) MAX_ENTITIES * 64L; // 64 bytes per mat4
            ssboMatricesId = device.createBuffer(size, flags);
            mappedMatricesPtr = device.mapBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, ssboMatricesId, 0L, size, flags);
            
            // Pre-bind texture unit locations
            sv.dark.rhi.DarkRHICommandList cmd = sv.dark.core.DarkRHIContext.get().getCommandList();
            cmd.bindPipeline(shaderProgramId);
            cmd.setUniform1i(diffuseLoc, 0); // GL_TEXTURE0
            cmd.setUniform1i(normalLoc, 1);  // GL_TEXTURE1
            cmd.setUniform1i(pbrLoc, 2);     // GL_TEXTURE2
            cmd.bindPipeline(0);

            DarkLogger.info("GRAPHICS", "Geometry Pass Shaders Compiled (AZDO Enabled).");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error initializing DarkGeometrySystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void beginPass(float[] viewMatrix, float[] projMatrix) {
        if (!isInitialized) return;
        try {
            sv.dark.rhi.DarkRHICommandList cmd = sv.dark.core.DarkRHIContext.get().getCommandList();
            cmd.bindPipeline(shaderProgramId);

            DarkRenderScratchpad.writeMatrix(viewMatrix);
            cmd.setUniformMatrix4fv(viewLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);

            DarkRenderScratchpad.writeMatrix(projMatrix);
            cmd.setUniformMatrix4fv(projLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);

            cmd.setUniform4f(colorMultLoc, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // Bind AZDO SSBO
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 0, ssboMatricesId);
            instanceCount = 0;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error beginning Geometry Pass", e);
        }
    }
    
    /**
     * AZDO: Writes directly to VRAM mapped memory without FFI crossing.
     * Zero-GC, Zero-Syscalls.
     */
    public static void setModelMatrix(float[] modelMatrix) {
        if (instanceCount >= MAX_ENTITIES) return;
        MemorySegment.copy(modelMatrix, 0, mappedMatricesPtr, ValueLayout.JAVA_FLOAT, instanceCount * 64L, 16);
        instanceCount++;
    }

    /**
     * Dispatches the instanced draw call for all accumulated matrices.
     */
    public static void flush(int indexCount) {
        if (instanceCount == 0 || !isInitialized) return;
        try {
            sv.dark.rhi.DarkRHICommandList cmd = sv.dark.core.DarkRHIContext.get().getCommandList();
            cmd.drawElementsInstanced(DarkRHI.PRIMITIVE_TRIANGLES, indexCount, DarkRHI.TYPE_UNSIGNED_INT, MemorySegment.NULL, instanceCount);
            instanceCount = 0; // Reset for next batch
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error flushing Geometry AZDO", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            device.unmapBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, ssboMatricesId);
            device.deleteBuffer(ssboMatricesId);
            device.deletePipeline(shaderProgramId);
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error destroying Geometry System", e);
        }
    }
}
