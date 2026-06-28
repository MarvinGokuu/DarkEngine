// Reading Order: 00010100
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.util;

import sv.dark.core.DarkLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * RESPONSIBILITY: Cross-Platform Native Library Resolver and Extractor.
 * WHY: Hardcoding ".dll" destroys portability. Loading from "lib/" breaks when packaging as a single JAR.
 * TECHNIQUE: Dynamically resolves OS and CPU Architecture. Extracts natives from Classpath to %TEMP% if needed.
 * GUARANTEES: Frictionless deployment. Zero manual DLL management for end users.
 * 
 * @author Marvin Alexander Flores Canales
 */
public final class NativeLibraryResolver {
    
    public enum OS { WINDOWS, LINUX, MACOS, UNKNOWN }
    public enum Architecture { X64, AARCH64, UNKNOWN }
    
    private static final OS CURRENT_OS;
    private static final Architecture CURRENT_ARCH;
    private static final String TEMP_DIR_PREFIX = "DarkEngine_Natives_" + UUID.randomUUID().toString().substring(0, 8);

    static {
        // 1. OS Detection
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            CURRENT_OS = OS.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            CURRENT_OS = OS.LINUX;
        } else if (osName.contains("mac")) {
            CURRENT_OS = OS.MACOS;
        } else {
            CURRENT_OS = OS.UNKNOWN;
        }

        // 2. CPU Architecture Detection
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            CURRENT_ARCH = Architecture.X64;
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            CURRENT_ARCH = Architecture.AARCH64;
        } else {
            CURRENT_ARCH = Architecture.UNKNOWN;
        }
    }

    public static OS getOS() {
        return CURRENT_OS;
    }

    public static Architecture getArch() {
        return CURRENT_ARCH;
    }

    /**
     * Resolves and prepares a native library for loading.
     * Searches in the local "lib/" folder first (Development Mode).
     * If not found, extracts it from the Classpath to a temporary folder (Production JAR Mode).
     * 
     * @param baseName The base name of the library (e.g., "glfw3", "soft_oal")
     * @return File pointer ready for System.load()
     */
    public static File resolveLibrary(String baseName) {
        String fileName = buildFileName(baseName);
        String archPath = getArchPath();
        
        // 1. Development Mode: Check local folder
        File localFile = new File("lib/" + fileName);
        if (localFile.exists()) {
            DarkLogger.info("RESOLVER", "Found local library: " + localFile.getAbsolutePath());
            return localFile;
        }

        // 2. Production Mode: Check Classpath (e.g. inside the JAR)
        // Expected structure inside JAR: /natives/windows-x64/glfw3.dll
        String classpathResource = "/natives/" + archPath + "/" + fileName;
        File extractedFile = extractFromClasspath(classpathResource, fileName);
        
        if (extractedFile != null) {
            return extractedFile;
        }
        
        // 3. Fallback: Return the local file path anyway, System.load will fail and throw an exception,
        // or the user might be relying on System.loadLibrary if they manipulated the java.library.path.
        DarkLogger.info("RESOLVER", "Library " + fileName + " not found locally or in classpath. Assuming system path.");
        return localFile; 
    }

    private static String buildFileName(String baseName) {
        switch (CURRENT_OS) {
            case WINDOWS:
                return baseName + ".dll";
            case LINUX:
                return "lib" + baseName + ".so";
            case MACOS:
                return "lib" + baseName + ".dylib";
            default:
                throw new UnsupportedOperationException("OS not supported for native library: " + baseName);
        }
    }

    private static String getArchPath() {
        String osPrefix = CURRENT_OS.name().toLowerCase();
        String archSuffix = CURRENT_ARCH == Architecture.AARCH64 ? "aarch64" : "x64";
        return osPrefix + "-" + archSuffix; // e.g. windows-x64, macos-aarch64
    }

    private static File extractFromClasspath(String resourcePath, String fileName) {
        try (InputStream is = NativeLibraryResolver.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return null;
            }

            // Create DarkEngine temp directory
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), TEMP_DIR_PREFIX);
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                // Try to clean up temp dir on exit
                tempDir.toFile().deleteOnExit();
            }

            File tempFile = tempDir.resolve(fileName).toFile();
            tempFile.deleteOnExit(); // Clean up library file on exit

            if (!tempFile.exists()) {
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
            }
            
            DarkLogger.info("RESOLVER", "Extracted library to: " + tempFile.getAbsolutePath());
            return tempFile;
            
        } catch (Exception e) {
            DarkLogger.error("RESOLVER", "Failed to extract native library: " + resourcePath);
            return null;
        }
    }
}
