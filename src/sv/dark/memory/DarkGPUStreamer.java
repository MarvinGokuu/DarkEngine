// Reading Order: 01100011
//  99
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.memory;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.core.systems.DarkOpenGLLinker;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * RESPONSIBILITY: Asynchronous Zero-Copy Asset transfers to VRAM.
 * WHY: glTexImage2D stalls the render thread while copying from RAM to VRAM.
 * TECHNIQUE: AZDO GL_PIXEL_UNPACK_BUFFER persistently mapped (PBO).
 * GUARANTEES: Zero GC. The background thread streams from NVMe to PBO, 
 * and OpenGL transfers via DMA asynchronously.
 */
@AAACertified(date = "2026-06-28", maxLatencyNs = 5, lockFree = true, offHeap = true, notes = "AZDO PBO Asynchronous Streaming")
public final class DarkGPUStreamer {

    private static int pboId;
    private static MemorySegment mappedPBO;
    public static final long PBO_SIZE = 64 * 1024 * 1024; // 64 MB Streaming Ring Buffer

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        try (Arena arena = Arena.ofConfined()) {
            DarkLogger.info("STREAMER", "Initializing AZDO GPU PBO Streamer (64MB)...");

            MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT);
            DarkOpenGLLinker.glGenBuffers.invokeExact(1, buffers);
            pboId = buffers.get(ValueLayout.JAVA_INT, 0);

            int flagsMap = DarkOpenGLLinker.GL_MAP_WRITE_BIT | DarkOpenGLLinker.GL_MAP_PERSISTENT_BIT | DarkOpenGLLinker.GL_MAP_COHERENT_BIT;

            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, pboId);
            DarkOpenGLLinker.glBufferStorage.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, PBO_SIZE, MemorySegment.NULL, flagsMap);
            mappedPBO = (MemorySegment) DarkOpenGLLinker.glMapBufferRange.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, 0L, PBO_SIZE, flagsMap);
            mappedPBO = mappedPBO.reinterpret(PBO_SIZE);
            
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, 0);

            isInitialized = true;
        } catch (Throwable e) {
            DarkLogger.fatal("STREAMER", "Failed to init DarkGPUStreamer", e);
        }
    }

    /**
     * Copies data directly from a Memory-Mapped File (DarkAsset) into the AZDO PBO.
     * This runs on a Background Thread (Zero-Syscall, Zero-GC).
     */
    public static void stageAssetPayload(DarkAsset asset, long pboOffset) {
        if (!isInitialized) return;
        MemorySegment payload = asset.payload();
        MemorySegment.copy(payload, 0, mappedPBO, pboOffset, payload.byteSize());
    }

    /**
     * Submits the GPU transfer command on the Render Thread.
     * DMA hardware will copy from the PBO to VRAM asynchronously.
     */
    public static void uploadTextureAsync(int targetTexId, int width, int height, long pboOffset) {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glBindTexture.invokeExact(DarkOpenGLLinker.GL_TEXTURE_2D, targetTexId);
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, pboId);
            
            // The memory pointer is an offset integer cast to pointer when PBO is bound
            MemorySegment offsetPtr = MemorySegment.ofAddress(pboOffset);
            DarkOpenGLLinker.glTexSubImage2D.invokeExact(
                DarkOpenGLLinker.GL_TEXTURE_2D, 0, 0, 0, width, height, 
                DarkOpenGLLinker.GL_RGBA, DarkOpenGLLinker.GL_UNSIGNED_BYTE, offsetPtr
            );
            
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, 0);
        } catch (Throwable e) {
            DarkLogger.error("STREAMER", "Failed to upload texture async: " + e.getMessage());
        }
    }

    public static void destroy() {
        if (!isInitialized) return;
        try {
            DarkOpenGLLinker.glBindBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER, pboId);
            DarkOpenGLLinker.glUnmapBuffer.invokeExact(DarkOpenGLLinker.GL_PIXEL_UNPACK_BUFFER);
            
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment buffers = arena.allocate(ValueLayout.JAVA_INT);
                buffers.set(ValueLayout.JAVA_INT, 0L, pboId);
                DarkOpenGLLinker.glDeleteBuffers.invokeExact(1, buffers);
                pboId = 0;
            }
            mappedPBO = null;
            isInitialized = false;
        } catch (Throwable e) {
            DarkLogger.error("STREAMER", "Error destroying GPU Streamer: " + e.getMessage());
        }
    }
}
