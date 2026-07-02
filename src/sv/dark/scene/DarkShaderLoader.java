// Reading Order: 00100018
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import sv.dark.core.DarkLogger;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for loading Shaders.
 * Supports hot-reloading from disk during development, 
 * and falls back to classpath loading in AAA+ distributed builds.
 */
public final class DarkShaderLoader {

    public static final java.util.concurrent.atomic.AtomicBoolean isShaderDirty = new java.util.concurrent.atomic.AtomicBoolean(false);

    private static java.nio.file.WatchService activeWatchService = null;

    public static void startHotReloadService() {
        try {
            activeWatchService = java.nio.file.FileSystems.getDefault().newWatchService();
            Path path = Path.of("src/sv/dark/scene");
            path.register(activeWatchService, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY);
            
            Thread.startVirtualThread(() -> {
                DarkLogger.info("GRAPHICS", "Shader Hot-Reload Service started on Virtual Thread.");
                try {
                    while (true) {
                        java.nio.file.WatchKey key = activeWatchService.take();
                        for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                            if (event.context().toString().endsWith(".comp")) {
                                isShaderDirty.set(true);
                                break;
                            }
                        }
                        key.reset();
                    }
                } catch (java.nio.file.ClosedWatchServiceException e) {
                    DarkLogger.info("GRAPHICS", "Hot-Reload Service stopped safely.");
                } catch (Exception e) {
                    DarkLogger.error("GRAPHICS", "Hot-Reload service failed: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            DarkLogger.error("GRAPHICS", "Failed to initialize WatchService: " + e.getMessage());
        }
    }

    public static void stopHotReloadService() {
        if (activeWatchService != null) {
            try {
                activeWatchService.close();
                activeWatchService = null;
            } catch (Exception e) {
                DarkLogger.error("GRAPHICS", "Failed to close WatchService: " + e.getMessage());
            }
        }
    }

    public static String loadShader(String relativePath) {
        try {
            Path diskPath = Path.of(relativePath);
            if (Files.exists(diskPath)) {
                return Files.readString(diskPath);
            }

            // AAA+ Distribution: Read from packaged JAR
            // Remove 'src/' from the path to match the classpath root
            String classpathPath = relativePath;
            if (relativePath.startsWith("src/")) {
                classpathPath = relativePath.substring(4);
            }
            if (relativePath.startsWith("src\\")) {
                classpathPath = relativePath.substring(4).replace('\\', '/');
            }

            InputStream is = DarkShaderLoader.class.getClassLoader().getResourceAsStream(classpathPath);
            if (is == null) {
                is = ClassLoader.getSystemResourceAsStream(classpathPath);
            }
            if (is == null) {
                throw new RuntimeException("Shader no encontrado ni en disco ni en classpath: " + relativePath);
            }

            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            DarkLogger.fatal("GRAPHICS", "Error cargando shader: " + relativePath, e);
            throw new RuntimeException("Error cargando shader", e);
        }
    }
}
