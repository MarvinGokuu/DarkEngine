// Reading Order: 00100021
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
            
            // 1. Compile cluster_grid.comp
            gridProgramId = compileComputeShader("src/sv/dark/scene/cluster_grid.comp");
            
            // 2. Compile light_culling.comp
            cullProgramId = compileComputeShader("src/sv/dark/scene/light_culling.comp");
            
            // 3. Create SSBOs
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, 3);
                DarkOpenGLLinker.glGenBuffers.invokeExact(3, buffers);
                clusterAABB_SSBO = buffers.get(ValueLayout.JAVA_INT, 0);
                lightGrid_SSBO = buffers.get(ValueLayout.JAVA_INT, 4);
                globalIndexCount_SSBO = buffers.get(ValueLayout.JAVA_INT, 8);
                
                // Cluster AABB Buffer (2 vec4 per cluster)
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, clusterAABB_SSBO);
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, (long)(NUM_CLUSTERS * 32), MemorySegment.NULL, DarkOpenGLLinker.GL_DYNAMIC_DRAW);
                
                // Light Grid Buffer
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, lightGrid_SSBO);
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, (long)(NUM_CLUSTERS * 8), MemorySegment.NULL, DarkOpenGLLinker.GL_DYNAMIC_DRAW);
                
                // Global Index Count Buffer (atomic counter + light index list)
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, globalIndexCount_SSBO);
                DarkOpenGLLinker.glBufferData.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, (long)(4 + (NUM_CLUSTERS * MAX_LIGHTS_PER_CLUSTER * 4)), MemorySegment.NULL, DarkOpenGLLinker.GL_DYNAMIC_DRAW);
                
                // Get Uniform Locations
                locInverseProj = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(gridProgramId, arena.allocateFrom("inverseProjection"));
                locScreenSize = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(gridProgramId, arena.allocateFrom("screenSize"));
                locZNear = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(gridProgramId, arena.allocateFrom("zNear"));
                locZFar = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(gridProgramId, arena.allocateFrom("zFar"));
                
                locViewMatrix = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(cullProgramId, arena.allocateFrom("viewMatrix"));
                locActiveLightCount = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(cullProgramId, arena.allocateFrom("activeLightCount"));
            }
            
            DarkLogger.info("GRAPHICS", "Clustered Shading Initialized.");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to init DarkClusteredSystem", e);
        }
    }

    private static int compileComputeShader(String path) throws Throwable {
        int shaderId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_COMPUTE_SHADER);
        String source = DarkShaderLoader.loadShader(path);
        
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment srcPtr = arena.allocateFrom(source);
            MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
            DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
        }
        
        DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
        
        int programId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
        DarkOpenGLLinker.glAttachShader.invokeExact(programId, shaderId);
        DarkOpenGLLinker.glLinkProgram.invokeExact(programId);
        DarkOpenGLLinker.glDeleteShader.invokeExact(shaderId);
        
        return programId;
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
            DarkOpenGLLinker.glUseProgram.invokeExact(gridProgramId);
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 1, clusterAABB_SSBO);
            
            // Set Uniforms — Zero-Alloc: use scratchpad instead of Arena.ofConfined()
            if (locInverseProj != -1) {
                // Compute actual matrix inverse of projMatrix
                if (!sv.dark.math.DarkMath.inverse(SCRATCH_INV_PROJ, projMatrix)) {
                    sv.dark.math.DarkMath.identity(SCRATCH_INV_PROJ);
                }
                DarkRenderScratchpad.writeMatrix(SCRATCH_INV_PROJ);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(locInverseProj, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }
            if (locScreenSize != -1) {
                DarkOpenGLLinker.glUniform2f.invokeExact(locScreenSize, 1280.0f, 720.0f);
            }
            if (locZNear != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(locZNear, 0.1f);
            }
            if (locZFar != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(locZFar, 1000.0f);
            }
            
            DarkOpenGLLinker.glDispatchCompute.invokeExact(1, 1, GRID_SIZE_Z);
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT);
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
            DarkOpenGLLinker.glUseProgram.invokeExact(cullProgramId);
            
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 1, clusterAABB_SSBO);
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 2, DarkLightSystem.getLightsSSBO());
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 3, globalIndexCount_SSBO);
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 4, lightGrid_SSBO);
            
            // Reset global atomic counter to 0 — Zero-Alloc: write int 0 into pre-allocated INT_4B.
            DarkRenderScratchpad.INT_4B.set(ValueLayout.JAVA_INT, 0L, 0);
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, globalIndexCount_SSBO);
            DarkOpenGLLinker.glBufferSubData.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, 4L, DarkRenderScratchpad.INT_4B);
            
            // Set uniforms — Zero-Alloc
            if (locActiveLightCount != -1) {
                DarkOpenGLLinker.glUniform1ui.invokeExact(locActiveLightCount, DarkLightSystem.getActiveLightCount());
            }
            if (locViewMatrix != -1) {
                DarkRenderScratchpad.writeMatrix(viewMatrix);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(locViewMatrix, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }
            
            DarkOpenGLLinker.glDispatchCompute.invokeExact(1, 1, GRID_SIZE_Z);
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BARRIER_BIT);
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
            if (gridProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(gridProgramId);
                gridProgramId = 0;
            }
            if (cullProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(cullProgramId);
                cullProgramId = 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT, 3);
                buffers.set(ValueLayout.JAVA_INT, 0L, clusterAABB_SSBO);
                buffers.set(ValueLayout.JAVA_INT, 4L, lightGrid_SSBO);
                buffers.set(ValueLayout.JAVA_INT, 8L, globalIndexCount_SSBO);
                DarkOpenGLLinker.glDeleteBuffers.invokeExact(3, buffers);
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
