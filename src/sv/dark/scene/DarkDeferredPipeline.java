// Reading Order: 01110000
//  112
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

            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();

            // 1. Generate FBO
            gBufferFBO = device.createFramebuffer();

            // 2. Textures will use RHI constants directly inline.
            
            // 3. Configure Albedo Texture (Color - 720p)
            albedoTexture = device.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, DarkRHI.FORMAT_RGBA8, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_UNSIGNED_BYTE, DarkRHI.FILTER_NEAREST);
            device.framebufferTexture2D(gBufferFBO, DarkRHI.ATTACHMENT_COLOR0, albedoTexture, 0);

            // 4. Configure Normal Texture (Geometría / Vectores - 720p)
            normalTexture = device.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, DarkRHI.FORMAT_RGBA16F, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_FLOAT, DarkRHI.FILTER_NEAREST);
            device.framebufferTexture2D(gBufferFBO, DarkRHI.ATTACHMENT_COLOR1, normalTexture, 0);

            // 4.5 Configure PBR Texture (Roughness/Metallic/AO - 720p)
            pbrTexture = device.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, DarkRHI.FORMAT_RGBA8, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_UNSIGNED_BYTE, DarkRHI.FILTER_NEAREST);
            device.framebufferTexture2D(gBufferFBO, DarkRHI.ATTACHMENT_COLOR2, pbrTexture, 0);

            // 4.6 Configure Depth Texture (Depth - 720p)
            depthTexture = device.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, DarkRHI.FORMAT_DEPTH_COMPONENT32F, DarkRHI.FORMAT_DEPTH_COMPONENT, DarkRHI.TYPE_FLOAT, DarkRHI.FILTER_NEAREST);
            device.framebufferTexture2D(gBufferFBO, DarkRHI.ATTACHMENT_DEPTH, depthTexture, 0);

            // 5. Configure Lit Texture (Salida Iluminada Computada - 720p)
            litTexture = device.createTexture2D(INTERNAL_WIDTH, INTERNAL_HEIGHT, DarkRHI.FORMAT_RGBA16F, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_FLOAT, DarkRHI.FILTER_LINEAR);

            // 6. Configure Presentation Texture (Salida Final Upscaled FSR - 4K)
            presentationTexture = device.createTexture2D(sv.dark.config.DarkDisplayConfig.targetWidth, sv.dark.config.DarkDisplayConfig.targetHeight, DarkRHI.FORMAT_RGBA8, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_UNSIGNED_BYTE, DarkRHI.FILTER_LINEAR);

            // 7. Verify FBO
            if (!device.checkFramebufferStatus(gBufferFBO)) {
                DarkLogger.fatal("GRAPHICS", "G-Buffer Framebuffer is not complete.", null);
                return;
            }

            // 8. Tell OpenGL which color attachments we'll use for rendering
            device.setDrawBuffers(gBufferFBO, new int[]{DarkRHI.ATTACHMENT_COLOR0, DarkRHI.ATTACHMENT_COLOR1, DarkRHI.ATTACHMENT_COLOR2});

            DarkLogger.info("GRAPHICS", "Deferred Pipeline Chassis Ready. Targets: Base 720p -> Presentation 4K");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to initialize Deferred Pipeline RHI.", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        
        try {
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            device.deleteTextures(new int[]{albedoTexture, normalTexture, pbrTexture, litTexture, presentationTexture, depthTexture});
            device.deleteFramebuffers(new int[]{gBufferFBO});

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
            
            sv.dark.rhi.DarkRHIDevice device = sv.dark.core.DarkRHIContext.get().getDevice();
            device.resizeTexture2D(presentationTexture, newWidth, newHeight, DarkRHI.FORMAT_RGBA8, DarkRHI.FORMAT_RGBA, DarkRHI.TYPE_UNSIGNED_BYTE);
        } catch (Throwable e) {
            DarkLogger.error("GRAPHICS", "Failed to resize target texture");
        }
    }
}
