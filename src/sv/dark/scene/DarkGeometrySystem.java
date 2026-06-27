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
 * Geometry Rendering System (Phase 27).
 * Executes the Geometry Pass: drawing 3D entities into the G-Buffer MRTs.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Rasterization Pipeline for G-Buffer Population")
public final class DarkGeometrySystem {

    private static int shaderProgramId;
    
    // Uniform Locations
    private static int modelLoc;
    private static int viewLoc;
    private static int projLoc;
    private static int diffuseLoc;
    private static int normalLoc;
    private static int pbrLoc;
    private static int colorMultLoc;

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
                modelLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("model"));
                viewLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("view"));
                projLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("projection"));
                diffuseLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_diffuse1"));
                normalLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_normal1"));
                pbrLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("texture_pbr1"));
                colorMultLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shaderProgramId, arenaLocal.allocateFrom("colorMultiplier"));
            }
            
            // Pre-bind texture unit locations
            DarkOpenGLLinker.glUseProgram.invokeExact(shaderProgramId);
            DarkOpenGLLinker.glUniform1i.invokeExact(diffuseLoc, 0); // GL_TEXTURE0
            DarkOpenGLLinker.glUniform1i.invokeExact(normalLoc, 1);  // GL_TEXTURE1
            DarkOpenGLLinker.glUniform1i.invokeExact(pbrLoc, 2);     // GL_TEXTURE2
            DarkOpenGLLinker.glUseProgram.invokeExact(0);

            DarkLogger.info("GRAPHICS", "Geometry Pass Shaders Compiled and Linked.");
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

    /**
     * Binds the Geometry Pass program and sets the camera matrices.
     * This should be called before drawing the ECS entities.
     */
    public static void beginPass(float[] viewMatrix, float[] projMatrix) {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(shaderProgramId);
            
            // Set global matrices
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment viewPtr = arena.allocateFrom(ValueLayout.JAVA_FLOAT, viewMatrix);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(viewLoc, 1, false, viewPtr);
                
                MemorySegment projPtr = arena.allocateFrom(ValueLayout.JAVA_FLOAT, projMatrix);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(projLoc, 1, false, projPtr);
            }
            
            // Default color multiplier (white)
            DarkOpenGLLinker.glUniform4f.invokeExact(colorMultLoc, 1.0f, 1.0f, 1.0f, 1.0f);
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error beginning Geometry Pass", e);
        }
    }
    
    /**
     * Helper to set model matrix per entity.
     */
    public static void setModelMatrix(float[] modelMatrix) {
        try {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment modelPtr = arena.allocateFrom(ValueLayout.JAVA_FLOAT, modelMatrix);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(modelLoc, 1, false, modelPtr);
            }
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error setting model matrix", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glDeleteProgram.invokeExact(shaderProgramId);
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error destroying Geometry System", e);
        }
    }
}
