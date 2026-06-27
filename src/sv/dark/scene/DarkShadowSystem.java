// Reading Order: 00100020
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
 * Cascaded Shadow Mapping (CSM) System.
 * Generates directional shadows using an FBO and a Depth Texture Array.
 */
@AAACertified(date = "2026-06-27", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native CSM Shadow Renderer")
public final class DarkShadowSystem {

    public static final int SHADOW_WIDTH = 2048;
    public static final int SHADOW_HEIGHT = 2048;
    public static final int CASCADE_COUNT = 3;

    private static int depthMapFBO;
    private static int depthMapArray;
    
    private static int shadowProgramId;
    private static int modelLoc;
    private static int lightSpaceMatrixLoc;
    
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try (Arena arena = Arena.ofConfined()) {
            DarkLogger.info("GRAPHICS", "Initializing Cascaded Shadow Mapping (CSM)...");
            
            // 1. Create FBO
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenFramebuffers.invokeExact(1, fboPtr);
            depthMapFBO = fboPtr.get(ValueLayout.JAVA_INT, 0);
            
            // 2. Create Texture Array
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenTextures.invokeExact(1, texPtr);
            depthMapArray = texPtr.get(ValueLayout.JAVA_INT, 0);
            
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, depthMapArray);
            DarkOpenGLLinker.glTexImage3D.invokeExact(
                DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, 0, DarkOpenGLLinker.GL_DEPTH_COMPONENT32F, 
                SHADOW_WIDTH, SHADOW_HEIGHT, CASCADE_COUNT, 
                0, DarkOpenGLLinker.GL_DEPTH_COMPONENT, DarkOpenGLLinker.GL_FLOAT, MemorySegment.NULL
            );
            
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_WRAP_S, DarkOpenGLLinker.GL_CLAMP_TO_BORDER);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_WRAP_T, DarkOpenGLLinker.GL_CLAMP_TO_BORDER);
            
            MemorySegment borderColor = arena.allocateFrom(ValueLayout.JAVA_FLOAT, new float[]{1.0f, 1.0f, 1.0f, 1.0f});
            DarkOpenGLLinker.glTexParameterfv.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkOpenGLLinker.GL_TEXTURE_BORDER_COLOR, borderColor);
            
            // 3. Attach Texture to FBO
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, depthMapFBO);
            DarkOpenGLLinker.glFramebufferTexture.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, DarkOpenGLLinker.GL_DEPTH_ATTACHMENT, depthMapArray, 0);
            
            // No color output needed
            DarkOpenGLLinker.glDrawBuffer.invokeExact(DarkOpenGLLinker.GL_NONE);
            DarkOpenGLLinker.glReadBuffer.invokeExact(DarkOpenGLLinker.GL_NONE);
            
            if ((int)DarkOpenGLLinker.glCheckFramebufferStatus.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER) != DarkOpenGLLinker.GL_FRAMEBUFFER_COMPLETE) {
                DarkLogger.fatal("GRAPHICS", "Shadow FBO is not complete", null);
            }
            
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
            
            // 4. Compile Shadow Shader
            int vertId = (int) DarkOpenGLLinker.glCreateShader.invokeExact(DarkOpenGLLinker.GL_VERTEX_SHADER);
            String vertSource = DarkShaderLoader.loadShader("src/sv/dark/scene/shadow_pass.vert");
            MemorySegment srcPtr = arena.allocateFrom(vertSource);
            MemorySegment srcArrayPtr = arena.allocateFrom(ValueLayout.ADDRESS, srcPtr);
            DarkOpenGLLinker.glShaderSource.invokeExact(vertId, 1, srcArrayPtr, MemorySegment.NULL);
            DarkOpenGLLinker.glCompileShader.invokeExact(vertId);
            
            shadowProgramId = (int) DarkOpenGLLinker.glCreateProgram.invokeExact();
            DarkOpenGLLinker.glAttachShader.invokeExact(shadowProgramId, vertId);
            DarkOpenGLLinker.glLinkProgram.invokeExact(shadowProgramId);
            DarkOpenGLLinker.glDeleteShader.invokeExact(vertId);
            
            modelLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shadowProgramId, arena.allocateFrom("model"));
            lightSpaceMatrixLoc = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(shadowProgramId, arena.allocateFrom("lightSpaceMatrix"));
            
            DarkLogger.info("GRAPHICS", "CSM System Initialized. Arrays: " + CASCADE_COUNT + "x" + SHADOW_WIDTH + "p");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to init DarkShadowSystem", e);
        }
    }

    public static void beginShadowPass(int cascadeIndex, float[] lightSpaceMatrix) {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glViewport.invokeExact(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, depthMapFBO);
            
            // Render specific layer of texture array
            DarkOpenGLLinker.glFramebufferTextureLayer.invokeExact(
                DarkOpenGLLinker.GL_FRAMEBUFFER, DarkOpenGLLinker.GL_DEPTH_ATTACHMENT, depthMapArray, 0, cascadeIndex
            );
            
            DarkOpenGLLinker.glClear.invokeExact(DarkOpenGLLinker.GL_DEPTH_BUFFER_BIT);
            
            DarkOpenGLLinker.glUseProgram.invokeExact(shadowProgramId);
            
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment matrixPtr = arena.allocateFrom(ValueLayout.JAVA_FLOAT, lightSpaceMatrix);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(lightSpaceMatrixLoc, 1, false, matrixPtr);
            }
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error in beginShadowPass");
        }
    }
    
    public static void setModelMatrix(float[] modelMatrix) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ptr = arena.allocateFrom(ValueLayout.JAVA_FLOAT, modelMatrix);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(modelLoc, 1, false, ptr);
        } catch (Throwable e) {}
    }

    public static void endShadowPass() {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);
        } catch (Throwable e) {}
    }
    
    public static int getDepthMapArray() {
        return depthMapArray;
    }
}
