// Reading Order: 01101110
//  110
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
package sv.dark.scene;

import sv.dark.core.systems.DarkOpenGLLinker;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Clustered Deferred Shading Compute System (Phase 29).
 * Divides the camera frustum into a 3D grid and assigns lights to each cluster.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader Clustered Shading")
public final class DarkClusteredSystem {

    private static int gridProgramId;
    private static int cullProgramId;
    
    private static int clusterAABB_SSBO;
    private static int lightGrid_SSBO;
    private static int globalIndexCount_SSBO;
    private static MemorySegment globalIndexMapped;
    
    // Uniform Locations
    private static int locInverseProj;
    private static int locScreenSize;
    private static int locZNear;
    private static int locZFar;
    private static int locViewMatrix;
    private static int locActiveLightCount;
    
    public static final int GRID_SIZE_X = 16;
    public static final int GRID_SIZE_Y = 9;
    public static final int GRID_SIZE_Z = 24;
    public static final int NUM_CLUSTERS = GRID_SIZE_X * GRID_SIZE_Y * GRID_SIZE_Z;
    public static final int MAX_LIGHTS_PER_CLUSTER = 100;

    /**
     * Pre-allocated scratch array for the inverse projection matrix.
     * WHY: dispatchGrid() was creating new float[16] every frame (Zero-GC violation).
     * Pre-allocating as a static field eliminates the heap allocation entirely.
     */
    private static final float[] SCRATCH_INV_PROJ = new float[16];

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try {
            DarkLogger.info("GRAPHICS", "Initializing Clustered Shading Compute Shaders...");
            
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            
            // 1. Compile cluster_grid.comp
            gridProgramId = device.createComputePipeline(DarkShaderLoader.loadShader("src/sv/dark/scene/cluster_grid.comp"));
            
            // 2. Compile light_culling.comp
            cullProgramId = device.createComputePipeline(DarkShaderLoader.loadShader("src/sv/dark/scene/light_culling.comp"));
            
            // 3. Create SSBOs
            clusterAABB_SSBO = device.createBuffer((long)(NUM_CLUSTERS * 32), 0);
            lightGrid_SSBO = device.createBuffer((long)(NUM_CLUSTERS * 8), 0);
            globalIndexCount_SSBO = device.createBuffer((long)(4 + (NUM_CLUSTERS * MAX_LIGHTS_PER_CLUSTER * 4)), sv.dark.rhi.DarkRHI.MAP_WRITE_BIT | sv.dark.rhi.DarkRHI.MAP_PERSISTENT_BIT | sv.dark.rhi.DarkRHI.MAP_COHERENT_BIT);
            
            // Map the first 4 bytes of globalIndexCount_SSBO to reset the atomic counter every frame (AZDO)
            globalIndexMapped = device.mapBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, globalIndexCount_SSBO, 0L, 4L, sv.dark.rhi.DarkRHI.MAP_WRITE_BIT | sv.dark.rhi.DarkRHI.MAP_PERSISTENT_BIT | sv.dark.rhi.DarkRHI.MAP_COHERENT_BIT);
            
            // Get Uniform Locations
            locInverseProj = device.getUniformLocation(gridProgramId, "inverseProjection");
            locScreenSize = device.getUniformLocation(gridProgramId, "screenSize");
            locZNear = device.getUniformLocation(gridProgramId, "zNear");
            locZFar = device.getUniformLocation(gridProgramId, "zFar");
            
            locViewMatrix = device.getUniformLocation(cullProgramId, "viewMatrix");
            locActiveLightCount = device.getUniformLocation(cullProgramId, "activeLightCount");
            
            DarkLogger.info("GRAPHICS", "Clustered Shading Initialized.");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to init DarkClusteredSystem", e);
        }
    }

    /**
     * Dispatches the Cluster Grid compute shader to build AABB clusters for the frustum.
     *
     * WHY Zero-Alloc: Previous impl created Arena.ofConfined() every frame for matrix/float
     * uniform uploads, causing mmap/munmap OS syscalls at 60 FPS. Now uses DarkRenderScratchpad
     * for zero-allocation uploads. SCRATCH_INV_PROJ replaces the new float[16] heap alloc.
     *
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     */
    public static void dispatchGrid(float[] projMatrix, float[] viewMatrix) {
        if (!isInitialized) return;
        try {
            sv.dark.rhi.DarkRHICommandList cmd = sv.dark.core.DarkRHIContext.get().getCommandList();
            cmd.bindPipeline(gridProgramId);
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 1, clusterAABB_SSBO);
            
            // Set Uniforms — Zero-Alloc: use scratchpad instead of Arena.ofConfined()
            if (locInverseProj != -1) {
                // Compute actual matrix inverse of projMatrix
                if (!sv.dark.math.DarkMath.inverse(SCRATCH_INV_PROJ, projMatrix)) {
                    sv.dark.math.DarkMath.identity(SCRATCH_INV_PROJ);
                }
                DarkRenderScratchpad.writeMatrix(SCRATCH_INV_PROJ);
                cmd.setUniformMatrix4fv(locInverseProj, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }
            if (locScreenSize != -1) {
                cmd.setUniform2f(locScreenSize, 1280.0f, 720.0f);
            }
            if (locZNear != -1) {
                cmd.setUniform1f(locZNear, 0.1f);
            }
            if (locZFar != -1) {
                cmd.setUniform1f(locZFar, 1000.0f);
            }
            
            cmd.dispatchCompute(1, 1, GRID_SIZE_Z);
            cmd.memoryBarrier(sv.dark.rhi.DarkRHI.BARRIER_SHADER_STORAGE);
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error dispatching Grid Compute");
        }
    }

    /**
     * Dispatches the Light Culling compute shader to assign lights to clusters.
     *
     * WHY Zero-Alloc: Previous impl had 2 Arena.ofConfined() per call (one for atomic counter
     * reset, one for viewMatrix upload). Replaced with DarkRenderScratchpad.INT_4B and
     * DarkRenderScratchpad.MATRIX_64B. Zero mmap/munmap syscalls at 60 FPS.
     *
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     */
    public static void dispatchCulling(float[] viewMatrix) {
        if (!isInitialized) return;
        try {
            sv.dark.rhi.DarkRHICommandList cmd = sv.dark.core.DarkRHIContext.get().getCommandList();
            cmd.bindPipeline(cullProgramId);
            
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 1, clusterAABB_SSBO);
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 2, DarkLightSystem.getLightsSSBO());
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 3, globalIndexCount_SSBO);
            cmd.bindBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, 4, lightGrid_SSBO);
            
            // Reset global atomic counter to 0 — Zero-Alloc: write int 0 directly to mapped memory
            globalIndexMapped.set(ValueLayout.JAVA_INT, 0L, 0); // Since it's persistently mapped directly here in init()!
            
            // Set uniforms — Zero-Alloc
            if (locActiveLightCount != -1) {
                cmd.setUniform1ui(locActiveLightCount, DarkLightSystem.getActiveLightCount());
            }
            if (locViewMatrix != -1) {
                DarkRenderScratchpad.writeMatrix(viewMatrix);
                cmd.setUniformMatrix4fv(locViewMatrix, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }
            
            cmd.dispatchCompute(1, 1, GRID_SIZE_Z);
            cmd.memoryBarrier(sv.dark.rhi.DarkRHI.BARRIER_SHADER_STORAGE);
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error dispatching Light Culling Compute");
        }
    }

    public static int getClusterAABB_SSBO() {
        return clusterAABB_SSBO;
    }

    public static int getLightGridSSBO() {
        return lightGrid_SSBO;
    }

    public static int getGlobalIndexCountSSBO() {
        return globalIndexCount_SSBO;
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            if (gridProgramId != 0) {
                device.deletePipeline(gridProgramId);
                gridProgramId = 0;
            }
            if (cullProgramId != 0) {
                device.deletePipeline(cullProgramId);
                cullProgramId = 0;
            }
            if (globalIndexCount_SSBO != 0 || clusterAABB_SSBO != 0 || lightGrid_SSBO != 0) {
                if (globalIndexCount_SSBO != 0) {
                    device.unmapBuffer(sv.dark.rhi.DarkRHI.BUFFER_TARGET_SSBO, globalIndexCount_SSBO);
                }
                device.deleteBuffers(new int[]{clusterAABB_SSBO, lightGrid_SSBO, globalIndexCount_SSBO});
                clusterAABB_SSBO = 0;
                lightGrid_SSBO = 0;
                globalIndexCount_SSBO = 0;
            }
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error destroying Clustered System resources: " + e.getMessage());
        }
    }
}
