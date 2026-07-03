// Reading Order: 00100015
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.core.systems.DarkOpenGLLinker;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * RESPONSIBILITY: Handle the creation and binding of the Deferred G-Buffer (Fase 27).
 * WHY: We need to decouple rendering from the screen's actual resolution to use FSR.
 * TECHNIQUE: FBOs (Framebuffer Objects) and multiple Texture targets.
 * GUARANTEES: Pre-allocated VRAM buffers. Zero GC during rendering.
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 1, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native G-Buffer Initialization via FFI")
public final class DarkDeferredPipeline {

    private static int gBufferFBO;
    private static int albedoTexture;
    private static int normalTexture;
    private static int pbrTexture;
    private static int litTexture;
    private static int presentationTexture;
    private static int depthTexture;
    
    // Resolución Base (Ajustable para performance vs calidad)
    public static final int INTERNAL_WIDTH = 1280;
    public static final int INTERNAL_HEIGHT = 720;
    
    // Resolución Final de Presentación (FSR Target) - Ahora en DarkDisplayConfig

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) {
            destroy();
        }

        try {
            DarkLogger.info("GRAPHICS", "Initializing Deferred G-Buffer (" + INTERNAL_WIDTH + "x" + INTERNAL_HEIGHT + ") in VRAM via RHI...");

            sv.dark.rhi.DarkRHI rhi = sv.dark.core.DarkRHIContext.get();

            // 1. Generate FBO
            gBufferFBO = rhi.createFramebuffer();
            rhi.bindFramebuffer(gBufferFBO);

            // 2. Generate Textures
            int GL_RGBA8 = 0x8058;
            int GL_RGBA = 0x1908;
            int GL_UNSIGNED_BYTE = 0x1401;
            int GL_NEAREST = 0x2600;
            int GL_LINEAR = 0x2601;
            int GL_RGBA16F = 0x881A;
            int GL_FLOAT = 0x1406;
            int GL_DEPTH_COMPONENT32F = 0x8CAC;
            int GL_DEPTH_COMPONENT = 0x1902;
            int GL_COLOR_ATTACHMENT0 = 0x8CE0;
            int GL_COLOR_ATTACHMENT1 = 0x8CE1;
            int GL_COLOR_ATTACHMENT2 = 0x8CE2;
            int GL_DEPTH_ATTACHMENT = 0x8D00;

            // 3. Configure Albedo Texture (Color - 720p)
            albedoTexture = rhi.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST);
            rhi.framebufferTexture2D(GL_COLOR_ATTACHMENT0, albedoTexture, 0);

            // 4. Configure Normal Texture (Geometría / Vectores - 720p)
            normalTexture = rhi.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_NEAREST);
            rhi.framebufferTexture2D(GL_COLOR_ATTACHMENT1, normalTexture, 0);

            // 4.5 Configure PBR Texture (Roughness/Metallic/AO - 720p)
            pbrTexture = rhi.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST);
            rhi.framebufferTexture2D(GL_COLOR_ATTACHMENT2, pbrTexture, 0);

            // 4.6 Configure Depth Texture (Depth - 720p)
            depthTexture = rhi.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, GL_NEAREST);
            rhi.framebufferTexture2D(GL_DEPTH_ATTACHMENT, depthTexture, 0);

            // 5. Configure Lit Texture (Salida Iluminada Computada - 720p)
            litTexture = rhi.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_LINEAR);

            // 6. Configure Presentation Texture (Salida Final Upscaled FSR - 4K)
            presentationTexture = rhi.createTexture2D(sv.dark.config.DarkDisplayConfig.targetWidth, sv.dark.config.DarkDisplayConfig.targetHeight, GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, GL_LINEAR);

            // 7. Verify FBO
            if (!rhi.checkFramebufferStatus()) {
                DarkLogger.fatal("GRAPHICS", "G-Buffer Framebuffer is not complete.", null);
                return;
            }

            // 8. Tell OpenGL which color attachments we'll use for rendering
            rhi.setDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2});

            // Unbind to return to default screen framebuffer
            rhi.bindFramebuffer(0);

            DarkLogger.info("GRAPHICS", "Deferred Pipeline Chassis Ready. Targets: Base 720p -> Presentation 4K");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to initialize Deferred Pipeline RHI.", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        
        try {
            sv.dark.rhi.DarkRHI rhi = sv.dark.core.DarkRHIContext.get();
            rhi.deleteTextures(new int[]{albedoTexture, normalTexture, pbrTexture, litTexture, presentationTexture, depthTexture});
            rhi.deleteFramebuffer(gBufferFBO);

            DarkLogger.info("GRAPHICS", "Deferred Pipeline VRAM Buffers Released.");
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to destroy Deferred Pipeline VRAM.", e);
        }
    }

    public static int getAlbedoTexture() {
        return albedoTexture;
    }

    public static int getNormalTexture() {
        return normalTexture;
    }

    public static int getLitTexture() {
        return litTexture;
    }

    public static int getPbrTexture() {
        return pbrTexture;
    }

    public static int getPresentationTexture() {
        return presentationTexture;
    }

    public static int getDepthTexture() {
        return depthTexture;
    }

    public static void resizeTarget(int newWidth, int newHeight) {
        if (!isInitialized) return;
        try {
            sv.dark.config.DarkDisplayConfig.setTargetResolution(newWidth, newHeight);
            DarkLogger.info("GRAPHICS", "Resizing FSR Target to " + newWidth + "x" + newHeight);
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, presentationTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA8, newWidth, newHeight, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, MemorySegment.NULL);
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Failed to resize target texture");
        }
    }
}
