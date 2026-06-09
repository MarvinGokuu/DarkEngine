package sv.dark.state;

import sv.dark.admin.AdminController;
import sv.dark.bus.DarkEventDispatcher;
import sv.dark.config.DarkEngineConfig;
import sv.dark.core.AAACertified;
import sv.dark.kernel.EngineKernel;
import sv.dark.memory.SectorMemoryVault;
import sv.dark.ui.AsyncLogWriter;
import sv.dark.ui.DarkEngineWindow;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Global Runtime Orchestration and Entry Point.
 * DEPENDENCIAS: EngineKernel, SectorMemoryVault, DarkEventDispatcher
 * MÉTRICAS: Tiempo de arranque <200ms
 * 
 * Single entry point (Main). Initializes off-heap memory vaults,
 * configures multi-lane architecture and transfers authority to the
 * Engine Kernel.
 * 
 * @author Marvin-Dev
 * @version 2.0 (AAA+ Refactor)
 * @since 2026-01-08
 */
@AAACertified(date = "2026-01-08", maxLatencyNs = 200_000_000, notes = "Engine Bootstrapper - Infrastructure Orchestrator")
public final class DarkEngineMaster {

    public static void main(String[] args) throws java.io.IOException {

        // ─── STEP 0: Redirigir stdout/stderr → darkengine.log (antes de cualquier
        // output) ───
        // Sin esto: cada System.out.println() en el hot-path del kernel = I/O
        // bloqueante.
        // Con esto: println() → ring buffer en memoria (nanosegundos), flush en daemon
        // thread.
        AsyncLogWriter logWriter = new AsyncLogWriter("darkengine.log");
        System.setOut(logWriter.createPrintStream(System.out));
        System.setErr(logWriter.createPrintStream(System.err));

        // ─── STEP 1: Ventana visual — aparece inmediatamente, antes del boot del
        // kernel ────
        DarkEngineWindow.launch();

        // ─── STEP 2: Init original (sin cambios)
        // ────────────────────────────────────────────
        // Force DarkEngineConfig class loading to trigger static block
        // initialization.
        // The static block prints the configuration banner and loads all config
        // settings.
        // Variable appears unused but its purpose is the side effect of class loading.
        @SuppressWarnings("unused")
        String profile = DarkEngineConfig.getProfile();

        System.out.println("DarkEngine v2.0");
        System.out.println("=================");
        System.out.println();

        // [NEURONA_048 STEP 1] SECTOR MEMORY VAULT (Off-Heap Memory)
        SectorMemoryVault memoryVault = new SectorMemoryVault(1024);

        // [NEURONA_048 STEP 2] EVENT DISPATCHER (Multi-Lane Bus)
        DarkEventDispatcher dispatcher = DarkEventDispatcher.createDefault(14);

        // [NEURONA_048 STEP 3] MAIN KERNEL (Central Processor)
        System.out.println("[ENGINE] Starting kernel...");
        EngineKernel kernel = new EngineKernel(dispatcher, memoryVault);

        // [NEURONA_048 STEP 4] ADMIN CONTROLLER (Control Plane)
        // Iniciar el Control Plane (HTTP Server + Admin Consumer)
        // Esto NO bloquea el hot-path, corre en threads separados
        AdminController.startControlPlane(kernel);

        // [NEURONA_048 STEP 5] CONFIGURE SYSTEMS
        configureSystems(kernel);

        // ─── STEP 3: Kernel en thread dedicado MAX_PRIORITY — preserva simpatía
        // mecánica ────
        // kernel.start() llama internamente a ThreadPinning.pinToCore(1).
        // Al correr en su propio thread MAX_PRIORITY, el OS lo favorece sobre el UI
        // thread
        // (NORM_PRIORITY - 1) y no comparte el ForkJoinPool con WorkStealingProcessor.
        Thread kernelThread = new Thread(() -> {
            try {
                kernel.start();
            } catch (Throwable t) {
                System.err.println("[MASTER] Kernel fatal: " + t.getMessage());
            }
        }, "dark-engine-kernel");
        kernelThread.setPriority(Thread.MAX_PRIORITY);
        kernelThread.setDaemon(false); // JVM no termina mientras el kernel corre
        kernelThread.start();
    }

    private static void configureSystems(EngineKernel kernel) {
        System.out.println("[ENGINE] Configuring User Systems...");
        var registry = kernel.getSystemRegistry();

        // Register Test Systems for Parallel Execution Validation
        registry.registerGameSystem(new sv.dark.test.SystemExecutionTest());
        registry.registerGameSystem(new sv.dark.test.SystemDependencyTest());
        registry.registerGameSystem(new sv.dark.test.SystemParallelismTest());

        // Finalize Dependency Graph
        registry.buildDependencyGraph();
        registry.setParallelMode(true);
    }
}
