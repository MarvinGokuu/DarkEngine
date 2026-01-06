package sv.volcan.state;

/**
 * AUTORIDAD: Volcan (Orquestación Global del Runtime)
 * RESPONSABILIDAD: Punto de entrada único, inicialización de bóvedas y ciclo de vida.
 * GARANTÍAS: Asegura que todos los subsistemas (Memory, Bus, State) estén alineados antes del primer tick.
 * PROHIBICIONES: No contiene lógica de simulación; solo delega a los Dispatchers.
 * DOMINIO CRÍTICO: Orquestación / Tiempo.
 */

import sv.volcan.kernel.SovereignKernel;
// Imports placeholder for future subsystems
import sv.volcan.core.memory.SectorMemoryVault;
import sv.volcan.bus.VolcanEventDispatcher;

public final class VolcanEngineMaster {

    public static void main(String[] args) {
        System.out.println("VOLCAN ENGINE v2.0 - SOVEREIGN BOOT");

        // 1. Inicialización de Memoria (Off-Heap Vaults)
        System.out.println("[master] Allocating SectorMemoryVault...");
        SectorMemoryVault.boot();

        // 2. Inicialización del Bus (Multi-Lane Architecture)
        System.out.println("[master] Init VolcanEventDispatcher (Multi-Lane)...");
        VolcanEventDispatcher dispatcher = VolcanEventDispatcher.createDefault(14); // 16K eventos por lane

        // 3. Transferencia de Autoridad al Kernel
        System.out.println("[master] Transferring Control to SovereignKernel...");
        SovereignKernel kernel = new SovereignKernel(dispatcher); // Inyección del dispatcher
        kernel.ignite();
    }
}
