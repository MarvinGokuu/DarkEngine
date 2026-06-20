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
                
                if (sourcePath.toLowerCase().endsWith(".png") || sourcePath.toLowerCase().endsWith(".jpg")) {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(source.toFile());
                    if (img == null) {
                        DarkLogger.error("COMPILER", "ImageIO failed to read image: " + sourcePath);
                        return;
                    }
                    
                    int width = img.getWidth();
                    int height = img.getHeight();
                    
                    // Extract raw RGBA
                    int[] pixels = new int[width * height];
                    img.getRGB(0, 0, width, height, pixels, 0, width);
                    
                    // Convert ARGB to RGBA
                    byte[] rgba = new byte[width * height * 4];
                    for (int i = 0; i < pixels.length; i++) {
                        int p = pixels[i];
                        rgba[i * 4] = (byte) ((p >> 16) & 0xFF);     // R
                        rgba[i * 4 + 1] = (byte) ((p >> 8) & 0xFF);  // G
                        rgba[i * 4 + 2] = (byte) (p & 0xFF);         // B
                        rgba[i * 4 + 3] = (byte) ((p >> 24) & 0xFF); // A
                    }
                    
                    // Write Header (18 bytes) + Payload
                    try (FileChannel destChannel = FileChannel.open(Paths.get(outPath), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        ByteBuffer header = ByteBuffer.allocateDirect(18).order(java.nio.ByteOrder.nativeOrder());
                        header.put("DARK\0".getBytes(StandardCharsets.US_ASCII));
                        header.put((byte) 1); // TYPE_TEXTURE2D
                        header.putInt(width);
                        header.putInt(height);
                        header.putInt(rgba.length);
                        header.flip();
                        
                        while (header.hasRemaining()) {
                            destChannel.write(header);
                        }
                        
                        // DMA write payload
                        ByteBuffer payload = ByteBuffer.wrap(rgba);
                        while (payload.hasRemaining()) {
                            destChannel.write(payload);
                        }
                    }
                    
                    DarkLogger.info("COMPILER", "Asset compiled successfully: " + outPath + " (" + width + "x" + height + ")");
                    
                    // Stream and queue for GPU upload
                    sv.dark.memory.DarkAsset asset = sv.dark.memory.DarkAssetStreamer.streamAsset(outPath, java.lang.foreign.Arena.global());
                    if (asset != null) {
                        sv.dark.kernel.EngineKernel.queueAssetForUpload(asset);
                    }
                } else {
                    DarkLogger.warning("COMPILER", "Unsupported asset type: " + sourcePath);
                }
                
            } catch (Exception e) {
                DarkLogger.error("COMPILER", "Failed to compile asset: " + e.getMessage());
            }
        });
    }
}
