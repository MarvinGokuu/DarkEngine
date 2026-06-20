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
