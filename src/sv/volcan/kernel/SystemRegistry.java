package sv.volcan.kernel;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.core.systems.VolcanRenderSystem;
import sv.volcan.state.WorldStateFrame;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * REGISTRO DE SISTEMAS (System Registry)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * AUTORIDAD: Sovereign Kernel
 * RESPONSABILIDAD: Gestionar el registro y ejecución de todos los sistemas del
 * motor.
 * 
 * PATRÓN: Registry Pattern + Strategy Pattern
 * PRINCIPIO SOLID: Open/Closed Principle
 * ROL: Software Architect
 * 
 * GARANTÍAS:
 * - Ejecución en orden determinista
 * - Separación entre lógica y renderizado
 * - Medición de tiempo por sistema
 * 
 * DOMINIO CRÍTICO: Kernel / Orquestación
 * 
 * @author MarvinDev
 * @version 1.0
 * @since 2026-01-03
 */
public final class SystemRegistry {

    // Sistemas de lógica de juego (ejecutan en el loop principal)
    private final List<SovereignSystem> gameSystems;

    // Sistemas de renderizado (ejecutan en thread de render)
    private final List<VolcanRenderSystem> renderSystems;

    // Métricas de performance
    private long lastExecutionTimeNs;

    public SystemRegistry() {
        // Usamos ArrayList por simplicidad
        // En producción, podríamos usar array fijo para zero-allocation
        this.gameSystems = new ArrayList<>(16);
        this.renderSystems = new ArrayList<>(8);
        this.lastExecutionTimeNs = 0;
    }

    /**
     * Registra un sistema de lógica de juego.
     * 
     * ORDEN: Los sistemas se ejecutan en el orden en que se registran.
     * DETERMINISMO: El orden debe ser consistente entre ejecuciones.
     * 
     * @param system Sistema a registrar
     */
    public void registerGameSystem(SovereignSystem system) {
        if (system == null) {
            throw new IllegalArgumentException("System cannot be null");
        }
        gameSystems.add(system);
        System.out.println("[REGISTRY] Registered game system: " + system.getName());
    }

    /**
     * Registra un sistema de renderizado.
     * 
     * @param system Sistema de render a registrar
     */
    public void registerRenderSystem(VolcanRenderSystem system) {
        if (system == null) {
            throw new IllegalArgumentException("System cannot be null");
        }
        renderSystems.add(system);
        System.out.println("[REGISTRY] Registered render system: " + system.getName());
    }

    /**
     * Ejecuta todos los sistemas de lógica de juego.
     * 
     * FASE 3 DEL LOOP: Systems Execution
     * 
     * GARANTÍAS:
     * - Orden determinista (mismo orden siempre)
     * - Mismo WorldStateFrame para todos
     * - Mismo deltaTime para todos
     * 
     * @param state     Estado del mundo (SSOT)
     * @param deltaTime Tiempo transcurrido en segundos
     */
    public void executeGameSystems(WorldStateFrame state, double deltaTime) {
        long startTime = System.nanoTime();

        // Ejecutar cada sistema en orden
        for (SovereignSystem system : gameSystems) {
            try {
                system.update(state, deltaTime);
            } catch (Exception e) {
                // En producción, esto debería ser más robusto
                System.err.println("[REGISTRY] Error in system " + system.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        long endTime = System.nanoTime();
        lastExecutionTimeNs = endTime - startTime;
    }

    /**
     * Ejecuta todos los sistemas de renderizado.
     * 
     * RENDER PHASE: Después del loop principal
     * 
     * @param g2d   Contexto gráfico
     * @param state Estado del mundo (Read-Only)
     */
    public void executeRenderSystems(Graphics2D g2d, WorldStateFrame state) {
        for (VolcanRenderSystem system : renderSystems) {
            try {
                system.render(g2d, state);
            } catch (Exception e) {
                System.err.println("[REGISTRY] Error in render system " + system.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Retorna el tiempo de ejecución de la última llamada a executeGameSystems().
     * 
     * @return Tiempo en nanosegundos
     */
    public long getLastExecutionTimeNs() {
        return lastExecutionTimeNs;
    }

    /**
     * Retorna el tiempo de ejecución en milisegundos.
     * 
     * @return Tiempo en milisegundos
     */
    public double getLastExecutionTimeMs() {
        return lastExecutionTimeNs / 1_000_000.0;
    }

    /**
     * Retorna el número de sistemas de lógica registrados.
     * 
     * @return Cantidad de sistemas
     */
    public int getGameSystemCount() {
        return gameSystems.size();
    }

    /**
     * Retorna el número de sistemas de render registrados.
     * 
     * @return Cantidad de sistemas
     */
    public int getRenderSystemCount() {
        return renderSystems.size();
    }
}
// Creado: 03/01/2026 23:50
// Autor: MarvinDev
// Patrón: Registry Pattern + Strategy Pattern
