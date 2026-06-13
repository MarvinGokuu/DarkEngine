// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.state;

import sv.dark.admin.AdminController;
import sv.dark.bus.DarkEventDispatcher;
import sv.dark.config.DarkEngineConfig;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.kernel.EngineKernel;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.ui.AsyncLogWriter;
import sv.dark.ui.DarkEngineWindow;
import sv.dark.core.systems.DarkInputSystem;
import sv.dark.core.systems.DarkAudioSystem;

/**
 * RESPONSIBILITY: Global Runtime Orchestration and Entry Point.
 * WHY: The engine requires a strict chronological bootstrap sequence to initialize native memory, the event bus, the visual window, and the background kernel thread safely.
 * TECHNIQUE: Single entry point (Main). Redirects I/O to async loggers, initializes off-heap memory vaults, configures multi-lane architecture, and pins the Kernel to a MAX_PRIORITY background thread.
 * GUARANTEES: Clean and predictable bootstrap sequence, isolating the kernel hot-path from AWT/OS thread interruptions.
 *
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-08", maxLatencyNs = 200_000_000, notes = "Engine Bootstrapper - Infrastructure Orchestrator")
public final class DarkEngineMaster {

    public static void main(String[] args) throws java.io.IOException {

        // -------------------------------------------------------------------------
        // STEP 0: Redirect stdout/stderr -> darkengine.log (before any output)
        // -------------------------------------------------------------------------
        // Without this: every System.out.println() in the kernel hot-path = blocking I/O.
        // With this: println() -> in-memory ring buffer (nanoseconds), flushed in daemon thread.
        AsyncLogWriter logWriter = new AsyncLogWriter("darkengine.log");
        System.setOut(logWriter.createPrintStream(System.out));
        System.setErr(logWriter.createPrintStream(System.err));

        // -------------------------------------------------------------------------
        // STEP 1: Visual Window - appears immediately, before kernel boot
        // -------------------------------------------------------------------------
        DarkEngineWindow.launch();

        // -------------------------------------------------------------------------
        // STEP 2: Original Init (unchanged)
        // -------------------------------------------------------------------------
        // Force DarkEngineConfig class loading to trigger static block
        // initialization.
        // The static block prints the configuration banner and loads all config
        // settings.
        // Variable appears unused but its purpose is the side effect of class loading.
        @SuppressWarnings("unused")
        String profile = DarkEngineConfig.getProfile();

        DarkLogger.info("ENGINE", "DarkEngine v2.0");
        DarkLogger.info("ENGINE", "=================");

        // [NEURONA_048 STEP 1] SECTOR MEMORY VAULT (Off-Heap Memory)
        SectorMemoryVault memoryVault = new SectorMemoryVault(1024);

        // [NEURONA_048 STEP 2] EVENT DISPATCHER (Multi-Lane Bus)
        DarkEventDispatcher dispatcher = DarkEventDispatcher.createDefault(14);

        // [NEURONA_048 STEP 3] MAIN KERNEL (Central Processor)
        DarkLogger.info("ENGINE", "Starting kernel...");
        EngineKernel kernel = new EngineKernel(dispatcher, memoryVault);

        // [NEURONA_048 STEP 4] ADMIN CONTROLLER (Control Plane)
        // Start the Control Plane (HTTP Server + Admin Consumer)
        // This DOES NOT block the hot-path, it runs in separate threads
        AdminController.startControlPlane(kernel, memoryVault);

        // [NEURONA_048 STEP 5] CONFIGURE SYSTEMS
        configureSystems(kernel, memoryVault);

        // -------------------------------------------------------------------------
        // STEP 3: Kernel in dedicated MAX_PRIORITY thread - preserves mechanical sympathy
        // -------------------------------------------------------------------------
        // kernel.start() internally calls ThreadPinning.pinToCore(1).
        // By running in its own MAX_PRIORITY thread, the OS favors it over the UI thread
        // (NORM_PRIORITY - 1) and does not share the ForkJoinPool with WorkStealingProcessor.
        Thread kernelThread = new Thread(() -> {
            try {
                kernel.start();
            } catch (Throwable t) {
                DarkLogger.fatal("MASTER", "Fatal Kernel Error", t);
            }
        }, "dark-engine-kernel");
        kernelThread.setPriority(Thread.MAX_PRIORITY);
        kernelThread.setDaemon(false); // JVM does not terminate while the kernel runs
        kernelThread.start();
    }

    private static void configureSystems(EngineKernel kernel, SectorMemoryVault memoryVault) {
        DarkLogger.info("ENGINE", "Configuring User Systems...");
        var registry = kernel.getSystemRegistry();

        // Register Test Systems for Parallel Execution Validation
        registry.registerGameSystem(new sv.dark.test.SystemExecutionTest());
        registry.registerGameSystem(new sv.dark.test.SystemDependencyTest());
        registry.registerGameSystem(new sv.dark.test.SystemParallelismTest());
        
        // Register Core Phase 2 Systems
        registry.registerGameSystem(new DarkInputSystem(memoryVault));
        registry.registerGameSystem(new DarkAudioSystem(memoryVault));

        // Finalize Dependency Graph
        registry.buildDependencyGraph();
        registry.setParallelMode(true);
    }
}
