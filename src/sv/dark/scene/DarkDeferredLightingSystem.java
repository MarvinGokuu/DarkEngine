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
import sv.dark.scene.DarkShaderLoader;
import sv.dark.scene.DarkRenderScratchpad;
import sv.dark.scene.DarkCameraState;
import sv.dark.math.DarkMath;

/**
 * Deferred Lighting System (Phase 27).
 * Reads the G-Buffer and applies lighting via Compute Shader.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader for Deferred Lighting")
public final class DarkDeferredLightingSystem {

    private static int computeProgramId;
    private static int envSSBO;
    private static MemorySegment envMemory;
    private static int camPosLocation;
    private static int invViewProjLocation;
    private static int viewMatrixLocation;
    private static int zNearLocation;
    private static int zFarLocation;
    private static int cascadeSplit0Location;
    private static int cascadeSplit1Location;
    private static int cascadeSplit2Location;
    private static int lightSpaceMatricesLocation;
    
    // Scratch matrices for World Position reconstruction in Shader
    private static final float[] tempViewProj = new float[16];
    private static final float[] tempInvViewProj = new float[16];
    
    // ECS Bindings (Updated per frame before dispatch)
    private static float[] currentSunDir = {0.5f, 1.0f, 0.5f};
    private static float[] currentSunColor = {1.5f, 1.425f, 1.2f};
    private static float[] currentCamPos = {0.0f, 5.0f, 10.0f};

    public static void init() {
        try {
            DarkLogger.info("GRAPHICS", "Inicializando Deferred Lighting System...");
            reloadShaders();
            
            try (Arena arenaLocal = Arena.ofConfined()) {
                // Initialize Environment SSBO (Binding 5) for Dynamic Lights
                MemorySegment bufferPtr = arenaLocal.allocate(ValueLayout.JAVA_INT);
                DarkOpenGLLinker.glGenBuffers.invokeExact(1, bufferPtr);
                envSSBO = bufferPtr.get(ValueLayout.JAVA_INT, 0);
                
                int flags = DarkOpenGLLinker.GL_MAP_WRITE_BIT | DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | DarkOpenGLLinker.GL_MAP_COHERENT_BIT;
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, envSSBO);
                DarkOpenGLLinker.glBufferStorage.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 32L, MemorySegment.NULL, flags);
                envMemory = (MemorySegment) DarkOpenGLLinker.glMapBufferRange.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 0L, 32L, flags);
                envMemory = envMemory.reinterpret(32L);

                MemorySegment nameCamPos = arenaLocal.allocateFrom("camPos");
                camPosLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, nameCamPos);

                MemorySegment nameInv = arenaLocal.allocateFrom("invViewProj");
                invViewProjLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, nameInv);

                viewMatrixLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("viewMatrix"));
                zNearLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("zNear"));
                zFarLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("zFar"));
                cascadeSplit0Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[0]"));
                cascadeSplit1Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[1]"));
                cascadeSplit2Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[2]"));
                lightSpaceMatricesLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("lightSpaceMatrices"));
            }
            
            DarkLogger.info("GRAPHICS", "Deferred Lighting inicializado.");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkDeferredLightingSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void reloadShaders() {
        try {
            if (computeProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(computeProgramId);
            }
            
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
            DarkOpenGLLinker.glDeleteShader.invokeExact(shaderId);
            
            try (Arena arenaLocal = Arena.ofConfined()) {
                camPosLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("camPos"));
                invViewProjLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("invViewProj"));
                viewMatrixLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("viewMatrix"));
                zNearLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("zNear"));
                zFarLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("zFar"));
                cascadeSplit0Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[0]"));
                cascadeSplit1Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[1]"));
                cascadeSplit2Location = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("cascadeSplits[2]"));
                lightSpaceMatricesLocation = (int) DarkOpenGLLinker.glGetUniformLocation.invokeExact(computeProgramId, arenaLocal.allocateFrom("lightSpaceMatrices"));
            }
            DarkLogger.info("GRAPHICS", "Deferred Lighting Compute Shader re-compilado exitosamente.");
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error en Hot-Reload de Shader: " + e.getMessage());
        }
    }

    /**
     * Called by the ECS / EngineKernel to update the current environmental lighting and camera.
     */
    public static void setEnvironment(float[] sunDir, float[] sunColor, float[] camPos) {
        if (sunDir != null) System.arraycopy(sunDir, 0, currentSunDir, 0, 3);
        if (sunColor != null) System.arraycopy(sunColor, 0, currentSunColor, 0, 3);
        if (camPos != null) System.arraycopy(camPos, 0, currentCamPos, 0, 3);
    }

    public static void dispatchLighting() {
        try {
            DarkOpenGLLinker.glUseProgram.invokeExact(computeProgramId);
            
            // Normalize sun direction to prevent lighting artifacts
            float length = (float) Math.sqrt(currentSunDir[0]*currentSunDir[0] + currentSunDir[1]*currentSunDir[1] + currentSunDir[2]*currentSunDir[2]);
            float sunX = currentSunDir[0] / length;
            float sunY = currentSunDir[1] / length;
            float sunZ = currentSunDir[2] / length;
            
            // Write Environment Data to SSBO (Zero-Syscall)
            envMemory.set(ValueLayout.JAVA_FLOAT, 0, sunX);
            envMemory.set(ValueLayout.JAVA_FLOAT, 4, sunY);
            envMemory.set(ValueLayout.JAVA_FLOAT, 8, sunZ);
            envMemory.set(ValueLayout.JAVA_FLOAT, 16, currentSunColor[0]);
            envMemory.set(ValueLayout.JAVA_FLOAT, 20, currentSunColor[1]);
            envMemory.set(ValueLayout.JAVA_FLOAT, 24, currentSunColor[2]);
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 5, envSSBO);
            DarkOpenGLLinker.glUniform3f.invokeExact(camPosLocation, currentCamPos[0], currentCamPos[1], currentCamPos[2]);

            // Upload camera view matrix
            if (viewMatrixLocation != -1) {
                DarkRenderScratchpad.writeMatrix(DarkCameraState.VIEW_MATRIX);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(viewMatrixLocation, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }

            // Upload near/far clip distances
            if (zNearLocation != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(zNearLocation, DarkCameraState.Z_NEAR);
            }
            if (zFarLocation != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(zFarLocation, DarkCameraState.Z_FAR);
            }

            // Calculate and upload CSM splits in view space (3 floats)
            float zNear = DarkCameraState.Z_NEAR;
            float zFar = DarkCameraState.Z_FAR;
            float split0 = zNear + DarkShadowSystem.CASCADE_SPLITS[1] * (zFar - zNear);
            float split1 = zNear + DarkShadowSystem.CASCADE_SPLITS[2] * (zFar - zNear);
            float split2 = zNear + DarkShadowSystem.CASCADE_SPLITS[3] * (zFar - zNear);
            if (cascadeSplit0Location != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(cascadeSplit0Location, split0);
            }
            if (cascadeSplit1Location != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(cascadeSplit1Location, split1);
            }
            if (cascadeSplit2Location != -1) {
                DarkOpenGLLinker.glUniform1f.invokeExact(cascadeSplit2Location, split2);
            }

            // Upload CSM light space matrices (3 matrices, 192 bytes = 48 floats)
            if (lightSpaceMatricesLocation != -1) {
                MemorySegment.copy(DarkShadowSystem.getCascadeLightMatrices(), 0, DarkRenderScratchpad.MATRIX_ARRAY_192B, ValueLayout.JAVA_FLOAT, 0L, 48);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(lightSpaceMatricesLocation, 3, false, DarkRenderScratchpad.MATRIX_ARRAY_192B);
            }

            // Calculate Inverse View-Projection Matrix
            DarkMath.multiplySIMD(tempViewProj, DarkCameraState.PROJ_MATRIX, DarkCameraState.VIEW_MATRIX);
            if (DarkMath.inverse(tempInvViewProj, tempViewProj)) {
                DarkRenderScratchpad.writeMatrix(tempInvViewProj);
                DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(invViewProjLocation, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }

            // Bind Albedo (texture unit 0)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE0);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getAlbedoTexture());
            
            // Bind Normal (texture unit 1)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE1);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getNormalTexture());

            // Bind PBR (texture unit 2)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE2);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getPbrTexture());

            // Bind Shadow Map Array Texture (texture unit 4)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE4);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D_ARRAY, DarkShadowSystem.getDepthMapArray());

            // Bind Depth (texture unit 5)
            DarkOpenGLLinker.glActiveTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE5);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkDeferredPipeline.getDepthTexture());

            // Bind Clustered Shading SSBOs to binding points 2, 3, 4
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 2, DarkLightSystem.getLightsSSBO());
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 3, DarkClusteredSystem.getGlobalIndexCountSSBO());
            DarkOpenGLLinker.glBindBufferBase.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, 4, DarkClusteredSystem.getLightGridSSBO());

            // Bind Lit output image (image unit 3)
            DarkOpenGLLinker.glBindImageTexture.invokeExact(3, DarkDeferredPipeline.getLitTexture(), 0, false, 0, DarkOpenGLLinker.GL_READ_WRITE, DarkOpenGLLinker.GL_RGBA16F);

            // Dispatch 1280x720 in 16x16 work groups
            int groupsX = (DarkDeferredPipeline.INTERNAL_WIDTH + 15) / 16;
            int groupsY = (DarkDeferredPipeline.INTERNAL_HEIGHT + 15) / 16;
            DarkOpenGLLinker.glDispatchCompute.invokeExact(groupsX, groupsY, 1);

            // Synchronize memory before FSR reads it
            DarkOpenGLLinker.glMemoryBarrier.invokeExact(DarkOpenGLLinker.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando Deferred Lighting", e);
        }
    }

    public static void destroy() {
        try {
            if (computeProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(computeProgramId);
                computeProgramId = 0;
            }
            if (envSSBO != 0) {
                DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER, envSSBO);
                DarkOpenGLLinker.glUnmapBuffer.invokeExact(DarkOpenGLLinker.GL_SHADER_STORAGE_BUFFER);
                try (Arena arenaLocal = Arena.ofConfined()) {
                    MemorySegment bufferPtr = arenaLocal.allocateFrom(ValueLayout.JAVA_INT, envSSBO);
                    DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, bufferPtr);
                }
                envSSBO = 0;
                envMemory = null;
            }
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error al destruir shader de Deferred Lighting", e);
        }
    }
}
