// Reading Order: 10011000
//  152
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;

import sv.dark.core.DarkLogger;
import sv.dark.core.DarkRHIContext;
import sv.dark.core.systems.DarkGraphicsLinker;
import sv.dark.scene.DarkDeferredPipeline;

public class SystemVRAMLeakTest {

    public static void main(String[] args) {
        DarkLogger.info("TEST", "==========================================================");
        DarkLogger.info("TEST", "  DARK ENGINE - VRAM ANTI-LEAK (FBO) STRESS TEST");
        DarkLogger.info("TEST", "==========================================================");

        try (Arena arena = Arena.ofConfined()) {
            // 1. Initialize GLFW Headless (Offscreen / Hidden Window)
            int initResult = (int) DarkGraphicsLinker.glfwInit.invokeExact();
            DarkGraphicsLinker.glfwWindowHint.invokeExact(131076, 0); // GLFW_VISIBLE = GLFW_FALSE
            
            MemorySegment titleStr = arena.allocateFrom("VRAM Leak Test");
            
            MemorySegment window = (MemorySegment) DarkGraphicsLinker.glfwCreateWindow.invokeExact(
                1280, 720, titleStr, MemorySegment.NULL, MemorySegment.NULL
            );
            
            if (window.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Failed to create GLFW window for test.");
            }

            DarkRHIContext.init();
            DarkRHIContext.get().getDevice().initializeContext(window);

            // 2. Stress Test FBO Lifecycle
            int iterations = 5000;
            DarkLogger.info("TEST", "Running " + iterations + " iterations of init() / destroy()...");
            
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                DarkDeferredPipeline.init();
                DarkDeferredPipeline.destroy();
                
                if (i % 1000 == 0 && i > 0) {
                    DarkLogger.info("TEST", "Passed " + i + " cycles without OOM.");
                }
            }
            long end = System.nanoTime();
            
            DarkLogger.info("TEST", "[OK] VRAM FBO/Texture Lifecycle Stress Test Passed.");
            DarkLogger.info("TEST", "Duration: " + ((end - start) / 1_000_000.0) + " ms");
            
            // Cleanup
            DarkGraphicsLinker.glfwDestroyWindow.invokeExact(window);
            DarkGraphicsLinker.glfwTerminate.invokeExact();
            
        } catch (Throwable t) {
            t.printStackTrace();
            DarkLogger.fatal("TEST", "[FAIL] VRAM Stress Test crashed.", t);
            System.exit(1);
        }
    }
}
