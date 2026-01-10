package sv.volcan.state;

import sv.volcan.core.AAACertified;
import sv.volcan.memory.SectorMemoryVault;
import sv.volcan.bus.VolcanEventDispatcher;
import sv.volcan.kernel.SovereignKernel;
import sv.volcan.admin.SovereignAdmin;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Orquestación Global del Runtime y Punto de Entrada.
 * DEPENDENCIAS: SovereignKernel, SectorMemoryVault, VolcanEventDispatcher
 * MÉTRICAS: Tiempo de arranque <200ms
 * 
 * Punto de entrada único (Main). Inicializa bóvedas de memoria off-heap,
 * configura la arquitectura multi-lane y transfiere autoridad al Kernel
 * Soberano.
 * 
 * @author Marvin-Dev
 * @version 2.0 (AAA+ Elite Refactor)
 * @since 2026-01-08
 */
@AAACertified(date = "2026-01-08", maxLatencyNs = 200_000_000, notes = "Sovereign Bootstrapper - Infrastructure Orchestrator")
public final class VolcanEngineMaster {

    public static void main(String[] args) throws java.io.IOException {
        System.out.println("VOLCAN ENGINE v2.0 - SOVEREIGN BOOT");

        // 1. Boot Subsystems (Memoria y Bus)
        System.out.println("[master] Allocating SectorMemoryVault...");
        // 2GB Capacity (32768 sectores de 64KB)

        SectorMemoryVault memoryVault = new SectorMemoryVault(32768);

        System.out.println("[master] Init VolcanEventDispatcher (Multi-Lane)...");
        VolcanEventDispatcher dispatcher = VolcanEventDispatcher.createDefault(14);

        // 2. Inicializar Kernel
        System.out.println("[master] Transferring Control to SovereignKernel...");
        SovereignKernel kernel = new SovereignKernel(dispatcher, memoryVault);

        // 3. Ignición de Infraestructura de Control (Async)
        // Movemos el hilo AdminConsumer a una clase interna o externa para limpiar el
        // Master
        System.out.println("[master] Igniting Control Plane...");
        SovereignAdmin.igniteControlPlane(kernel);

        // 4. Configuración de Sistemas (Capa de Usuario)
        configureSystems(kernel);

        // 5. Transferencia de Autoridad Final
        kernel.ignite();
    }

    private static void configureSystems(SovereignKernel kernel) {
        System.out.println("[master] Configuring User Systems...");
        var registry = kernel.getSystemRegistry();

        // Register Test Systems for Parallel Execution Validation
        registry.registerGameSystem(new sv.volcan.test.TestSystemA());
        registry.registerGameSystem(new sv.volcan.test.TestSystemB());
        registry.registerGameSystem(new sv.volcan.test.TestSystemC());

        // Finalize Dependency Graph
        registry.buildDependencyGraph();
        registry.setParallelMode(true);
    }
}
