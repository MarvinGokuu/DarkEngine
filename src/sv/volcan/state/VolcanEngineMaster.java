package sv.volcan.state;

import sv.volcan.admin.AdminController;
import sv.volcan.bus.VolcanEventDispatcher;
import sv.volcan.config.VolcanEngineConfig;
import sv.volcan.core.AAACertified;
import sv.volcan.kernel.EngineKernel;
import sv.volcan.memory.SectorMemoryVault;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Global Runtime Orchestration and Entry Point.
 * DEPENDENCIAS: EngineKernel, SectorMemoryVault, VolcanEventDispatcher
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
public final class VolcanEngineMaster {

    public static void main(String[] args) throws java.io.IOException {
        // Force VolcanEngineConfig class loading to trigger static block
        // initialization.
        // The static block prints the configuration banner and loads all config
        // settings.
        // Variable appears unused but its purpose is the side effect of class loading.
        @SuppressWarnings("unused")
        String profile = VolcanEngineConfig.getProfile();

        System.out.println("VolcanEngine v2.0");
        System.out.println("=================");
        System.out.println();

        // [NEURONA_048 STEP 1] SECTOR MEMORY VAULT (Off-Heap Memory)
        SectorMemoryVault memoryVault = new SectorMemoryVault(1024);

        // [NEURONA_048 STEP 2] EVENT DISPATCHER (Multi-Lane Bus)
        VolcanEventDispatcher dispatcher = VolcanEventDispatcher.createDefault(14);

        // [NEURONA_048 STEP 3] MAIN KERNEL (Central Processor)
        System.out.println("[ENGINE] Starting kernel...");
        EngineKernel kernel = new EngineKernel(dispatcher, memoryVault);

        // [NEURONA_048 STEP 4] ADMIN CONTROLLER (Control Plane)
        // Iniciar el Control Plane (HTTP Server + Admin Consumer)
        // Esto NO bloquea el hot-path, corre en threads separados
        AdminController.startControlPlane(kernel);

        // [NEURONA_048 STEP 5] CONFIGURE SYSTEMS
        configureSystems(kernel);

        // [NEURONA_048 STEP 6] START KERNEL
        kernel.start();
    }

    private static void configureSystems(EngineKernel kernel) {
        System.out.println("[ENGINE] Configuring User Systems...");
        var registry = kernel.getSystemRegistry();

        // Register Test Systems for Parallel Execution Validation
        registry.registerGameSystem(new sv.volcan.test.SystemExecutionTest());
        registry.registerGameSystem(new sv.volcan.test.SystemDependencyTest());
        registry.registerGameSystem(new sv.volcan.test.SystemParallelismTest());

        // Finalize Dependency Graph
        registry.buildDependencyGraph();
        registry.setParallelMode(true);
    }
}
