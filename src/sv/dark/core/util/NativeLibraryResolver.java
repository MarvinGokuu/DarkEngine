// Reading Order: 00010100
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.util;

import sv.dark.core.DarkLogger;
import java.io.File;

/**
 * RESPONSIBILITY: Cross-Platform Native Library Resolver.
 * WHY: Hardcoding ".dll" destroys portability. This class dynamically resolves the correct extension and naming 
 * convention for Windows, Linux, and macOS.
 * 
 * @author Marvin Alexander Flores Canales
 */
public final class NativeLibraryResolver {
    
    public enum OS { WINDOWS, LINUX, MACOS, UNKNOWN }
    
    private static final OS CURRENT_OS;

    static {
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
    }

    public static OS getOS() {
        return CURRENT_OS;
    }

    public static File resolveLibrary(String baseName) {
        String fileName;
        
        switch (CURRENT_OS) {
            case WINDOWS:
                fileName = baseName + ".dll";
                break;
            case LINUX:
                // Handling standard Linux naming (lib[name].so)
                if (baseName.equals("glfw3")) fileName = "libglfw.so.3";
                else if (baseName.equals("soft_oal")) fileName = "libopenal.so.1";
                else fileName = "lib" + baseName + ".so";
                break;
            case MACOS:
                // Handling standard MacOS naming (lib[name].dylib)
                if (baseName.equals("glfw3")) fileName = "libglfw.3.dylib";
                else if (baseName.equals("soft_oal")) fileName = "libopenal.dylib";
                else fileName = "lib" + baseName + ".dylib";
                break;
            default:
                throw new UnsupportedOperationException("OS not supported for native library: " + baseName);
        }
        
        File libFile = new File("lib/" + fileName);
        if (!libFile.exists() && CURRENT_OS != OS.WINDOWS) {
            DarkLogger.info("RESOLVER", "Library " + fileName + " not found in lib/ folder. Assuming system path.");
        }
        return libFile;
    }
}
