package sv.dark.editor;

import sv.dark.core.DarkLogger;
import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * RESPONSIBILITY: Offline compilation of heavy assets into flat binaries.
 * WHY: Parsing .fbx or .png at runtime freezes the engine.
 * TECHNIQUE: Background thread processing, writing raw bytes to .darkasset.
 */
public class DarkAssetCompiler {
    
    /**
     * Compiles an asset in a non-blocking virtual thread.
     */
    public static void compileAsync(String sourcePath) {
        Thread.startVirtualThread(() -> {
            try {
                Path source = Paths.get(sourcePath);
                String outPath = sourcePath + ".darkasset";
                
                DarkLogger.info("COMPILER", "Compiling asset offline: " + source.getFileName());
                
                // SIMULATION of Phase 21 Asset Compiler:
                // We read the raw file, and write it to our custom flat binary format via DMA (Zero-GC).
                // In production (Fase 27), this would strip PNG headers and write pure RGBA.
                try (FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.READ);
                     FileChannel destChannel = FileChannel.open(Paths.get(outPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                     
                    long size = sourceChannel.size();
                    
                    // Write Header
                    ByteBuffer header = ByteBuffer.allocateDirect(9);
                    header.put("DARK\0".getBytes(StandardCharsets.US_ASCII));
                    header.putInt((int) size);
                    header.flip();
                    
                    while (header.hasRemaining()) {
                        destChannel.write(header);
                    }
                    
                    // Direct DMA Transfer (Zero-GC, Zero-Heap Allocation)
                    long position = 0;
                    while (position < size) {
                        position += sourceChannel.transferTo(position, size - position, destChannel);
                    }
                }
                
                DarkLogger.info("COMPILER", "Asset compiled successfully: " + outPath);
                
                // Immediately stream it into memory as proof of Zero-Copy Pipeline
                sv.dark.memory.DarkAssetStreamer.streamAsset(outPath, java.lang.foreign.Arena.global());
                
            } catch (Exception e) {
                DarkLogger.error("COMPILER", "Failed to compile asset: " + e.getMessage());
            }
        });
    }
}
