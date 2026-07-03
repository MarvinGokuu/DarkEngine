package sv.dark.scene;

import sv.dark.core.DarkRHIContext;
import sv.dark.rhi.DarkRHI;
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
 * Deferred Lighting System (Phase 28 RHI).
 * Reads the G-Buffer and applies lighting via Compute Shader.
 */
@AAACertified(date = "2026-07-02", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Native Compute Shader for Deferred Lighting via RHI")
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
            DarkLogger.info("GRAPHICS", "Inicializando Deferred Lighting System (RHI)...");
            reloadShaders();
            
            DarkRHI rhi = DarkRHIContext.get();
            int flags = DarkRHI.MAP_WRITE_BIT | DarkRHI.MAP_PERSISTENT_BIT | DarkRHI.MAP_COHERENT_BIT;
            envSSBO = rhi.createBuffer(32L, flags);
            envMemory = rhi.mapBuffer(DarkRHI.BUFFER_TARGET_SSBO, envSSBO, 0L, 32L, flags);

            camPosLocation = rhi.getUniformLocation(computeProgramId, "camPos");
            invViewProjLocation = rhi.getUniformLocation(computeProgramId, "invViewProj");
            viewMatrixLocation = rhi.getUniformLocation(computeProgramId, "viewMatrix");
            zNearLocation = rhi.getUniformLocation(computeProgramId, "zNear");
            zFarLocation = rhi.getUniformLocation(computeProgramId, "zFar");
            cascadeSplit0Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[0]");
            cascadeSplit1Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[1]");
            cascadeSplit2Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[2]");
            lightSpaceMatricesLocation = rhi.getUniformLocation(computeProgramId, "lightSpaceMatrices");
            
            DarkLogger.info("GRAPHICS", "Deferred Lighting inicializado.");

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error inicializando DarkDeferredLightingSystem", e);
            throw new RuntimeException(e);
        }
    }

    public static void reloadShaders() {
        try {
            DarkRHI rhi = DarkRHIContext.get();
            if (computeProgramId != 0) {
                rhi.deleteProgram(computeProgramId);
            }
            
            String source = DarkShaderLoader.loadShader("src/sv/dark/scene/deferred_lighting.comp");
            computeProgramId = rhi.createComputeShader(source);
            
            camPosLocation = rhi.getUniformLocation(computeProgramId, "camPos");
            invViewProjLocation = rhi.getUniformLocation(computeProgramId, "invViewProj");
            viewMatrixLocation = rhi.getUniformLocation(computeProgramId, "viewMatrix");
            zNearLocation = rhi.getUniformLocation(computeProgramId, "zNear");
            zFarLocation = rhi.getUniformLocation(computeProgramId, "zFar");
            cascadeSplit0Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[0]");
            cascadeSplit1Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[1]");
            cascadeSplit2Location = rhi.getUniformLocation(computeProgramId, "cascadeSplits[2]");
            lightSpaceMatricesLocation = rhi.getUniformLocation(computeProgramId, "lightSpaceMatrices");
            
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
            DarkRHI rhi = DarkRHIContext.get();
            rhi.useProgram(computeProgramId);
            
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
            rhi.bindBufferBase(DarkRHI.BUFFER_TARGET_SSBO, 5, envSSBO);
            rhi.setUniform3f(camPosLocation, currentCamPos[0], currentCamPos[1], currentCamPos[2]);

            // Upload camera view matrix
            if (viewMatrixLocation != -1) {
                DarkRenderScratchpad.writeMatrix(DarkCameraState.VIEW_MATRIX);
                rhi.setUniformMatrix4fv(viewMatrixLocation, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }

            // Upload near/far clip distances
            if (zNearLocation != -1) {
                rhi.setUniform1f(zNearLocation, DarkCameraState.Z_NEAR);
            }
            if (zFarLocation != -1) {
                rhi.setUniform1f(zFarLocation, DarkCameraState.Z_FAR);
            }

            // Calculate and upload CSM splits in view space (3 floats)
            float zNear = DarkCameraState.Z_NEAR;
            float zFar = DarkCameraState.Z_FAR;
            float split0 = zNear + DarkShadowSystem.CASCADE_SPLITS[1] * (zFar - zNear);
            float split1 = zNear + DarkShadowSystem.CASCADE_SPLITS[2] * (zFar - zNear);
            float split2 = zNear + DarkShadowSystem.CASCADE_SPLITS[3] * (zFar - zNear);
            if (cascadeSplit0Location != -1) {
                rhi.setUniform1f(cascadeSplit0Location, split0);
            }
            if (cascadeSplit1Location != -1) {
                rhi.setUniform1f(cascadeSplit1Location, split1);
            }
            if (cascadeSplit2Location != -1) {
                rhi.setUniform1f(cascadeSplit2Location, split2);
            }

            // Upload CSM light space matrices (3 matrices, 192 bytes = 48 floats)
            if (lightSpaceMatricesLocation != -1) {
                MemorySegment.copy(DarkShadowSystem.getCascadeLightMatrices(), 0, DarkRenderScratchpad.MATRIX_ARRAY_192B, ValueLayout.JAVA_FLOAT, 0L, 48);
                rhi.setUniformMatrix4fv(lightSpaceMatricesLocation, 3, false, DarkRenderScratchpad.MATRIX_ARRAY_192B);
            }

            // Calculate Inverse View-Projection Matrix
            DarkMath.multiplySIMD(tempViewProj, DarkCameraState.PROJ_MATRIX, DarkCameraState.VIEW_MATRIX);
            if (DarkMath.inverse(tempInvViewProj, tempViewProj)) {
                DarkRenderScratchpad.writeMatrix(tempInvViewProj);
                rhi.setUniformMatrix4fv(invViewProjLocation, 1, false, DarkRenderScratchpad.MATRIX_64B);
            }

            // Bind Albedo (texture unit 0)
            rhi.activeTexture(0);
            rhi.bindTexture2D(DarkDeferredPipeline.getAlbedoTexture());
            
            // Bind Normal (texture unit 1)
            rhi.activeTexture(1);
            rhi.bindTexture2D(DarkDeferredPipeline.getNormalTexture());

            // Bind PBR (texture unit 2)
            rhi.activeTexture(2);
            rhi.bindTexture2D(DarkDeferredPipeline.getPbrTexture());

            // Bind Shadow Map Array Texture (texture unit 4)
            rhi.activeTexture(4);
            rhi.bindTexture2DArray(DarkShadowSystem.getDepthMapArray());

            // Bind Depth (texture unit 5)
            rhi.activeTexture(5);
            rhi.bindTexture2D(DarkDeferredPipeline.getDepthTexture());

            // Bind Clustered Shading SSBOs to binding points 2, 3, 4
            rhi.bindBufferBase(DarkRHI.BUFFER_TARGET_SSBO, 2, DarkLightSystem.getLightsSSBO());
            rhi.bindBufferBase(DarkRHI.BUFFER_TARGET_SSBO, 3, DarkClusteredSystem.getGlobalIndexCountSSBO());
            rhi.bindBufferBase(DarkRHI.BUFFER_TARGET_SSBO, 4, DarkClusteredSystem.getLightGridSSBO());

            // Bind Lit output image (image unit 3)
            rhi.bindImageTexture(3, DarkDeferredPipeline.getLitTexture(), 0, false, 0, DarkRHI.ACCESS_READ_WRITE, DarkRHI.FORMAT_RGBA16F);

            // Dispatch 1280x720 in 16x16 work groups
            int groupsX = (DarkDeferredPipeline.INTERNAL_WIDTH + 15) / 16;
            int groupsY = (DarkDeferredPipeline.INTERNAL_HEIGHT + 15) / 16;
            rhi.dispatchCompute(groupsX, groupsY, 1);

            // Synchronize memory before FSR reads it
            rhi.memoryBarrier(DarkRHI.BARRIER_SHADER_IMAGE);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error despachando Deferred Lighting", e);
        }
    }

    public static void destroy() {
        try {
            DarkRHI rhi = DarkRHIContext.get();
            if (rhi == null) return;
            
            if (computeProgramId != 0) {
                rhi.deleteProgram(computeProgramId);
                computeProgramId = 0;
            }
            if (envSSBO != 0) {
                rhi.unmapBuffer(DarkRHI.BUFFER_TARGET_SSBO, envSSBO);
                rhi.deleteBuffer(envSSBO);
                envSSBO = 0;
                envMemory = null;
            }
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Error al destruir shader de Deferred Lighting", e);
        }
    }
}
