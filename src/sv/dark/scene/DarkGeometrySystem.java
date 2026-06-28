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
            
            // Compile Vertex Shader
            int vertId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_VERTEX_SHADER);
            String vertSource = DarkShaderLoader.loadShader("src/sv/dark/scene/geometry_pass.vert");
            compileShader(vertId, vertSource);
            
            // Compile Fragment Shader
            int fragId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_FRAGMENT_SHADER);
            String fragSource = DarkShaderLoader.loadShader("src/sv/dark/scene/geometry_pass.frag");
            compileShader(fragId, fragSource);
            
            shaderProgramId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(shaderProgramId, vertId);
            DarkOpenGLLinker.glAttachShader.invokeExact(shaderProgramId, fragId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(shaderProgramId);
            
            // Cleanup individual shaders
            DarkOpenGLLinker.glDeleteShader.invokeExact(vertId);
            DarkOpenGLLinker.glDeleteShader.invokeExact(fragId);

            try (Arena arenaLocal = Arena.ofConfined()) {
                viewLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("view"));
                projLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("projection"));
                diffuseLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_diffuse1"));
                normalLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_normal1"));
                pbrLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_pbr1"));
                colorMultLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("colorMultiplier"));
                
                // AZDO SSBO Initialization
                MemorySegment idPtr = arenaLocal.allocate(ValueLayout.JAVA_INT);
                DarkOpenGLLinker.glGenBuffers.invokeExact(1, idPtr);
                ssboMatricesId = idPtr.get(ValueLayout.JAVA_INT, 0);
                
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboMatricesId);
                long size = (long) MAX_ENTITIES * 64L; // 64 bytes per mat4
                int flags = DarkOpenGLLinker.GL_MAP_WRITE_BIT | DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
                DarkOpenGLLinker.glBufferStorage.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, size, MemorySegment.NULL, flags);
                
                mappedMatricesPtr = (MemorySegment) DarkOpenGLLinker.glMapBufferRange.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, size, flags);
                mappedMatricesPtr = mappedMatricesPtr.reinterpret(size); // Allow Java to write up to size
            }
            
            // Pre-bind texture unit locations
            DarkOpenGLLinker.glUseProgram.invokeExact(shaderProgramId);
            DarkOpenGLLinker.glUniform1i.invokeExact(diffuseLoc, 0); // GL_TEXTURE0
            DarkOpenGLLinker.glUniform1i.invokeExact(normalLoc, 1);  // GL_TEXTURE1
            DarkOpenGLLinker.glUniform1i.invokeExact(pbrLoc, 2);     // GL_TEXTURE2
            DarkOpenGLLinker.glUseProgram.invokeExact(0);

            DarkLogger.info("GRAPHICS", "Geometry Pass Shaders Compiled (AZDO Enabled).");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error initializing DarkGeometrySystem", e);
            throw new RuntimeException(e);
        }
    }

    private static void compileShader(int shaderId, String source) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment srcPtr = arena.allocateFrom(source);
            MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
            DarkOpenGLLinker.glShaderSource.invokeExact(shaderId, 1, srcArrayPtr, MemorySegment.NULL);
            DarkOpenGLLinker.glCompileShader.invokeExact(shaderId);
            
            MemorySegment statusPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGetShaderiv.invokeExact(shaderId, DarkOpenGLLinker.GL_COMPILE_STATUS, statusPtr);
            if (statusPtr.get(ValueLayout.JAVA_INT, 0) == 0) {
                MemorySegment logPtr = arena.allocate(1024);
                DarkOpenGLLinker.glGetShaderInfoLog.invokeExact(shaderId, 1024, MemorySegment.NULL, logPtr);
                String error = logPtr.getString(0);
                DarkLogger.fatal("GRAPHICS", "Shader compilation failed: " + error, null);
            }
        }
    }

    public static void beginPass(float[] viewMatrix, float[] projMatrix) {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(shaderProgramId);

            DarkRenderScratchpad.writeMatrix(viewMatrix);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(viewLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);

            DarkRenderScratchpad.writeMatrix(projMatrix);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(projLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);

            DarkOpenGLLinker.glUniform4f.invokeExact(colorMultLoc, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // Bind AZDO SSBO
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0, ssboMatricesId);
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
            DarkOpenGLLinker.glDrawElementsInstanced.invokeExact(DarkOpenGLLinker.GL_TRIANGLES, indexCount, DarkOpenGLLinker.GL_UNSIGNED_INT, MemorySegment.NULL, instanceCount);
            instanceCount = 0; // Reset for next batch
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error flushing Geometry AZDO", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, ssboMatricesId);
            DarkOpenGLLinker.glUnmapBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
            
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment ptr = arena.allocate(ValueLayout.JAVA_INT);
                ptr.set(ValueLayout.JAVA_INT, 0, ssboMatricesId);
                DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, ptr);
            }
            
            DarkOpenGLLinker.glDeleteProgram.invokeExact(shaderProgramId);
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error destroying Geometry System", e);
        }
    }
}
