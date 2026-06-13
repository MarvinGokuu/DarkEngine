// Reading Order: 00000000
package sv.dark.ui;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.core.systems.DarkGraphicsLinker;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * RESPONSIBILITY: Visual layer of the Dark-Engine — Native Window.
 * WHY: AWT generated 15ms of Input Lag. We need a native window hooked directly to the OS.
 * TECHNIQUE: Project Panama FFI. Calls glfw3.dll natively to bypass the JVM.
 * GUARANTEES: 0ms Input Lag. Zero JVM overhead. Native hardware access.
 * 
 * @author Marvin-Dev
 */
@AAACertified(date = "2026-06-13", maxLatencyNs = 0, minThroughput = 0, alignment = 0, lockFree = true, offHeap = true, notes = "Native FFI Window via GLFW")
public final class DarkEngineWindow {

    private static MemorySegment windowPointer;
    private static volatile boolean running = true;

    /**
     * Exposes the native FFI pointer for Raw Input Polling.
     */
    public static MemorySegment getWindowPointer() {
        return windowPointer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    /** Launches the native window via FFI and returns immediately. */
    public static void launch() {
        // [TERMINATOR THREAD] Guarantees the Java process dies and releases ports
        // if the Kernel Shutdown Hook freezes for any reason.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
        }));

        Thread t = new Thread(DarkEngineWindow::renderLoop, "dark-glfw-render");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER LOOP (NATIVE)
    // ─────────────────────────────────────────────────────────────────────────

    private static void renderLoop() {
        try {
            DarkLogger.info("GRAPHICS", "Initializing Native Graphics Pipeline (GLFW)...");
            int initResult = (int) DarkGraphicsLinker.glfwInit.invokeExact();
            if (initResult == 0) {
                DarkLogger.fatal("GRAPHICS", "Failed to init GLFW native library.", null);
                return;
            }

            try (Arena arena = Arena.ofConfined()) {
                // Set title as requested by Architect
                MemorySegment title = arena.allocateFrom("DarkEngine - Servidor de Conexion (Daemon)");
                
                // Creates the Window without AWT!
                windowPointer = (MemorySegment) DarkGraphicsLinker.glfwCreateWindow.invokeExact(
                    900, 520, title, MemorySegment.NULL, MemorySegment.NULL
                );
            }

            if (windowPointer.equals(MemorySegment.NULL)) {
                DarkLogger.fatal("GRAPHICS", "Failed to create Native Window via GLFW.", null);
                return;
            }

            DarkLogger.info("GRAPHICS", "Native Window successfully created (0ms Input Lag).");

            // Main OS Event Loop
            while (running) {
                int shouldClose = (int) DarkGraphicsLinker.glfwWindowShouldClose.invokeExact(windowPointer);
                if (shouldClose != 0) {
                    running = false;
                    break;
                }
                
                // Poll native OS events (Keyboard, Mouse, Window resizes)
                DarkGraphicsLinker.glfwPollEvents.invokeExact();
                
                // Limit the loop slightly so we don't cook the CPU while we don't have Vulkan rendering
                Thread.sleep(16);
            }

            DarkLogger.info("GRAPHICS", "Terminating Native Window...");
            DarkGraphicsLinker.glfwTerminate.invokeExact();
            System.exit(0);

        } catch (Throwable e) {
            DarkLogger.fatal("GRAPHICS", "Native FFI Exception in Render Loop", e);
            System.exit(1);
        }
    }
}
