package sv.dark.memory;

import java.lang.foreign.MemorySegment;

/**
 * Encapsulates a Zero-Copy asset mapped directly from disk to RAM.
 * Ready for DMA injection to VRAM.
 */
public record DarkAsset(
    int type,           // 1 = Texture2D
    int width,          // Texture width
    int height,         // Texture height
    int payloadSize,    // Size of the raw data payload
    MemorySegment payload // Pointer to the isolated payload data (Zero-Copy)
) {}
