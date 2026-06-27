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
    
    // Resolución Base (Ajustable para performance vs calidad)
    public static final int INTERNAL_WIDTH = 1280;
    public static final int INTERNAL_HEIGHT = 720;
    
    // Resolución Final de Presentación (FSR Target)
    public static final int TARGET_WIDTH = sv.dark.config.DarkEngineConfig.GRAPHICS_TARGET_WIDTH;
    public static final int TARGET_HEIGHT = sv.dark.config.DarkEngineConfig.GRAPHICS_TARGET_HEIGHT;

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) {
            destroy();
        }

        try (Arena arena = Arena.ofConfined()) {
            DarkLogger.info("GRAPHICS", "Initializing Deferred G-Buffer (" + INTERNAL_WIDTH + "x" + INTERNAL_HEIGHT + ") in VRAM...");

            // 1. Generate FBO
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenFramebuffers.invokeExact(1, fboPtr);
            gBufferFBO = fboPtr.get(ValueLayout.JAVA_INT, 0);
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, gBufferFBO);

            // 2. Generate Textures (5 texturas ahora)
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT, 5);
            DarkOpenGLLinker.glGenTextures.invokeExact(5, texPtr);
            albedoTexture = texPtr.get(ValueLayout.JAVA_INT, 0);
            normalTexture = texPtr.get(ValueLayout.JAVA_INT, 4);
            pbrTexture = texPtr.get(ValueLayout.JAVA_INT, 8);
            litTexture = texPtr.get(ValueLayout.JAVA_INT, 12);
            presentationTexture = texPtr.get(ValueLayout.JAVA_INT, 16);

            // 3. Configure Albedo Texture (Color - 720p)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, albedoTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA8, INTERNAL_WIDTH, INTERNAL_HEIGHT, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glFramebufferTexture2D.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, DarkOpenGLLinker.GL_COLOR_ATTACHMENT0, DarkOpenGLLinker.GL_TEXTURE_2D, albedoTexture, 0);

            // 4. Configure Normal Texture (Geometría / Vectores - 720p)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, normalTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA16F, INTERNAL_WIDTH, INTERNAL_HEIGHT, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_FLOAT, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glFramebufferTexture2D.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, DarkOpenGLLinker.GL_COLOR_ATTACHMENT1, DarkOpenGLLinker.GL_TEXTURE_2D, normalTexture, 0);

            // 4.5 Configure PBR Texture (Roughness/Metallic/AO - 720p)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, pbrTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA8, INTERNAL_WIDTH, INTERNAL_HEIGHT, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_NEAREST);
            DarkOpenGLLinker.glFramebufferTexture2D.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, DarkOpenGLLinker.GL_COLOR_ATTACHMENT2, DarkOpenGLLinker.GL_TEXTURE_2D, pbrTexture, 0);

            // 5. Configure Lit Texture (Salida Iluminada Computada - 720p)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, litTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA16F, INTERNAL_WIDTH, INTERNAL_HEIGHT, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_FLOAT, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_LINEAR);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_LINEAR);

            // 6. Configure Presentation Texture (Salida Final Upscaled FSR - 4K)
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, presentationTexture);
            DarkOpenGLLinker.glTexImage2D.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, 0, DarkOpenGLLinker.GL_RGBA8, TARGET_WIDTH, TARGET_HEIGHT, 0, DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, MemorySegment.NULL);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MIN_FILTER, DarkOpenGLLinker.GL_LINEAR);
            DarkOpenGLLinker.glTexParameteri.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, DarkOpenGLLinker.GL_TEXTURE_MAG_FILTER, DarkOpenGLLinker.GL_LINEAR);

            // 7. Verify FBO
            int status = (int) DarkOpenGLLinker.glCheckFramebufferStatus.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER);
            if (status != DarkOpenGLLinker.GL_FRAMEBUFFER_COMPLETE) {
                DarkLogger.fatal("GRAPHICS", "G-Buffer Framebuffer is not complete. Status: " + status, null);
                return;
            }

            // 8. Tell OpenGL which color attachments we'll use for rendering
            MemorySegment drawBuffers = arena.allocate(ValueLayout.JAVA_INT, 3);
            drawBuffers.set(ValueLayout.JAVA_INT, 0, DarkOpenGLLinker.GL_COLOR_ATTACHMENT0);
            drawBuffers.set(ValueLayout.JAVA_INT, 4, DarkOpenGLLinker.GL_COLOR_ATTACHMENT1);
            drawBuffers.set(ValueLayout.JAVA_INT, 8, DarkOpenGLLinker.GL_COLOR_ATTACHMENT2);
            DarkOpenGLLinker.glDrawBuffers.invokeExact(3, drawBuffers);

            // Unbind to return to default screen framebuffer
            DarkOpenGLLinker.glBindFramebuffer.invokeExact(DarkOpenGLLinker.GL_FRAMEBUFFER, 0);

            DarkLogger.info("GRAPHICS", "Deferred Pipeline Chassis Ready. Targets: Base 720p -> Presentation 4K");
            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to initialize Deferred Pipeline FFI.", e);
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        
        try (Arena arena = Arena.ofConfined()) {
            // Eliminar texturas
            MemorySegment texPtr = arena.allocate(ValueLayout.JAVA_INT, 5);
            texPtr.set(ValueLayout.JAVA_INT, 0, albedoTexture);
            texPtr.set(ValueLayout.JAVA_INT, 4, normalTexture);
            texPtr.set(ValueLayout.JAVA_INT, 8, pbrTexture);
            texPtr.set(ValueLayout.JAVA_INT, 12, litTexture);
            texPtr.set(ValueLayout.JAVA_INT, 16, presentationTexture);
            DarkOpenGLLinker.glDeleteTextures.invokeExact(5, texPtr);

            // Eliminar FBO
            MemorySegment fboPtr = arena.allocate(ValueLayout.JAVA_INT);
            fboPtr.set(ValueLayout.JAVA_INT, 0, gBufferFBO);
            DarkOpenGLLinker.glDeleteFramebuffers.invokeExact(1, fboPtr);

            DarkLogger.info("GRAPHICS", "Deferred Pipeline VRAM Buffers Released.");
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Failed to destroy Deferred Pipeline VRAM FFI.", e);
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
}
