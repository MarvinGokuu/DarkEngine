package sv.dark.editor;

import sv.dark.core.DarkLogger;
import java.io.*;
import java.nio.file.*;

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
                // We read the raw file, and write it to our custom flat binary format.
                // In production (Fase 27), this would strip PNG headers and write pure RGBA.
                byte[] rawData = Files.readAllBytes(source);
                
                try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(outPath))) {
                    dos.writeBytes("DARK\0"); // 5 byte Magic Header
                    dos.writeInt(rawData.length); // 4 byte Size
                    dos.write(rawData); // Raw Data Payload
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
