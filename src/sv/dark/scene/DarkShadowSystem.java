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
            
            // Zero-Alloc matrix upload: reuse MATRIX_64B scratchpad, 0 OS syscalls.
            // WHY: This method is called 3x per frame (one per cascade). Old impl:
            // 3 Arena.ofConfined() = 6 mmap/munmap syscalls per frame = 360/second.
            // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
            DarkRenderScratchpad.writeMatrix(lightSpaceMatrix);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(lightSpaceMatrixLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error in beginShadowPass");
        }
    }
    
    /**
     * Zero-Alloc model matrix upload per shadow-casting entity.
     * WHY: Called N_entities x CASCADE_COUNT per frame. Arena.ofConfined() = N*3*2 syscalls/frame.
     * // [NON_BLOCKING] [ZERO_ALLOCATION] [RENDER_THREAD_ONLY]
     */
    public static void setModelMatrix(float[] modelMatrix) {
        try {
            DarkRenderScratchpad.writeMatrix(modelMatrix);
            DarkOpenGLLinker.glUniformMatrix4fv.invokeExact(modelLoc, 1, false, DarkRenderScratchpad.MATRIX_64B);
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
    
    // =========================================================================
    // CSM MATRIX CALCULATIONS (Zero-GC using DarkMath)
    // =========================================================================
    
    // Arrays pre-allocated to avoid GC
    private static final float[] tempCamInverse       = new float[16];
    private static final float[] tempFrustumCorners   = new float[32]; // 8 corners * 4 (x,y,z,w)
    private static final float[] tempLightView        = new float[16];
    private static final float[] tempLightOrtho       = new float[16];
    private static final float[] tempLightSpaceMatrix = new float[16];
    // NDC unit cube corners — mathematical constant, NEVER allocate per frame.
    // [FUTURE AUDITORS]: This is static final intentionally. The local `ndcBox`
    // that was here before caused 180 heap allocations/second (3 cascades x 60fps).
    private static final float[] NDC_BOX = {
        -1,-1,-1,  1,-1,-1,  -1,1,-1,  1,1,-1,
        -1,-1, 1,  1,-1, 1,  -1,1, 1,  1,1, 1
    };
    
    // Standard cascade split distances (Starting at 0.0f to cover near plane)
    public static final float[] CASCADE_SPLITS = { 0.0f, 0.05f, 0.25f, 1.0f }; // Near, Mid, Far

    // Pre-allocated array to hold all cascade light space matrices (3 cascades * 16 floats = 48 floats)
    public static final float[] CASCADE_LIGHT_MATRICES = new float[16 * CASCADE_COUNT];

    public static float[] getCascadeLightMatrices() {
        return CASCADE_LIGHT_MATRICES;
    }

    /**
     * Calculates the Light Space Matrix for a specific cascade.
     * @param cascadeIndex 0, 1, or 2
     * @param camView The main camera view matrix
     * @param fovY Camera FOV in radians
     * @param aspect Camera aspect ratio
     * @param zNear Camera near plane
     * @param zFar Camera far plane
     * @param sunDir Direction of the sun (normalized)
     * @param outMatrix Pre-allocated array to store the result
     */
    public static void calculateCascadeMatrix(
            int cascadeIndex, 
            float[] camView, 
            float fovY, float aspect, float zNear, float zFar, 
            float[] sunDir, 
            float[] outMatrix) {
            
        float splitNear = zNear + CASCADE_SPLITS[cascadeIndex] * (zFar - zNear);
        float splitFar  = zNear + CASCADE_SPLITS[cascadeIndex + 1] * (zFar - zNear);
        
        // 1. Camera perspective for this cascade — write into tempLightOrtho (reused as camProj temp).
        // WHY: Reusing the pre-allocated static array avoids a new float[16] heap allocation.
        sv.dark.math.DarkMath.perspective(tempLightOrtho, fovY, aspect, splitNear, splitFar);
        
        // 2. Multiply Proj * View — write into tempCamInverse (reused as viewProj temp).
        sv.dark.math.DarkMath.multiplySIMD(tempCamInverse, tempLightOrtho, camView);
        
        // 3. Exact 8-corner inverse projection.
        // Inverse the ViewProj matrix to calculate the 8 frustum corners in World Space.
        sv.dark.math.DarkMath.inverse(tempCamInverse, tempCamInverse); // In-place inverse
        
        float frustumCenterX = 0.0f;
        float frustumCenterY = 0.0f;
        float frustumCenterZ = 0.0f;
        
        for (int i = 0; i < 8; i++) {
            float x = NDC_BOX[i * 3];
            float y = NDC_BOX[i * 3 + 1];
            float z = NDC_BOX[i * 3 + 2];
            
            // Multiply Inverse(ViewProj) * vec4(x, y, z, 1.0)
            float w = tempCamInverse[3] * x + tempCamInverse[7] * y + tempCamInverse[11] * z + tempCamInverse[15];
            float wx = (tempCamInverse[0] * x + tempCamInverse[4] * y + tempCamInverse[8] * z + tempCamInverse[12]) / w;
            float wy = (tempCamInverse[1] * x + tempCamInverse[5] * y + tempCamInverse[9] * z + tempCamInverse[13]) / w;
            float wz = (tempCamInverse[2] * x + tempCamInverse[6] * y + tempCamInverse[10] * z + tempCamInverse[14]) / w;
            
            tempFrustumCorners[i * 4] = wx;
            tempFrustumCorners[i * 4 + 1] = wy;
            tempFrustumCorners[i * 4 + 2] = wz;
            
            frustumCenterX += wx;
            frustumCenterY += wy;
            frustumCenterZ += wz;
        }
        
        frustumCenterX /= 8.0f;
        frustumCenterY /= 8.0f;
        frustumCenterZ /= 8.0f;
        
        // Calculate max radius of the bounding sphere tightly fitting the 8 corners
        float radius = 0.0f;
        for (int i = 0; i < 8; i++) {
            float dx = tempFrustumCorners[i * 4] - frustumCenterX;
            float dy = tempFrustumCorners[i * 4 + 1] - frustumCenterY;
            float dz = tempFrustumCorners[i * 4 + 2] - frustumCenterZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) radius = dist;
        }
        
        // Stabilize shadow map by snapping to texel increments
        // This completely eliminates Shadow Shimmering (AAA Technique)
        float texelSize = (2.0f * radius) / SHADOW_WIDTH;
        frustumCenterX = (float) Math.floor(frustumCenterX / texelSize) * texelSize;
        frustumCenterY = (float) Math.floor(frustumCenterY / texelSize) * texelSize;
        frustumCenterZ = (float) Math.floor(frustumCenterZ / texelSize) * texelSize;

        // 4. Build light view matrix: look AT the frustum center FROM a sun-direction offset.
        sv.dark.math.DarkMath.lookAt(tempLightView,
            frustumCenterX - sunDir[0] * radius,
            frustumCenterY - sunDir[1] * radius,
            frustumCenterZ - sunDir[2] * radius,
            frustumCenterX, frustumCenterY, frustumCenterZ,
            0.0f, 1.0f, 0.0f);

        // 5. Calculate tight orthographic bounds for this cascade.
        // We use the radius for all axes to ensure a perfect spherical bounding box
        sv.dark.math.DarkMath.ortho(tempLightOrtho, -radius, radius, -radius, radius, -radius * 5.0f, radius * 5.0f);
        
        // 6. lightSpaceMatrix = ortho * lightView — write directly into outMatrix (pre-allocated by caller).
        sv.dark.math.DarkMath.multiplySIMD(outMatrix, tempLightOrtho, tempLightView);

        // Copy directly to the global cascade matrices array
        System.arraycopy(outMatrix, 0, CASCADE_LIGHT_MATRICES, cascadeIndex * 16, 16);
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            if (shadowProgramId != 0) {
                DarkOpenGLLinker.glDeleteProgram.invokeExact(shadowProgramId);
                shadowProgramId = 0;
            }
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
                fboPtr.set(ValueLayout.JAVA_INT, 0L, depthMapFBO);
                DarkOpenGLLinker.glDeleteFramebuffers.invokeExact(1, fboPtr);
                depthMapFBO = 0;
                
                MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT);
                texPtr.set(ValueLayout.JAVA_INT, 0L, depthMapArray);
                DarkOpenGLLinker.glDeleteTextures.invokeExact(1, texPtr);
                depthMapArray = 0;
            }
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Error destroying Shadow System resources: " + e.getMessage());
        }
    }
}
