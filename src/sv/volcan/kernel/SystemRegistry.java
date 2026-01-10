package sv.volcan.kernel;

import sv.volcan.core.systems.SovereignSystem;
import sv.volcan.core.systems.VolcanRenderSystem;
import sv.volcan.state.WorldStateFrame;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Registro y Orquestación de Sistemas (Logic & Render).
 * DEPENDENCIAS: SovereignSystem, VolcanRenderSystem
 * MÉTRICAS: O(N) Execution, Zero-GC en Runtime
 * 
 * Implementa el patrón Registry + Strategy para gestionar el ciclo de vida
 * y la ejecución ordenada de todos los sistemas del motor.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class SystemRegistry {

    // Sistemas de lógica de juego (ejecutan en el loop principal)
    private final List<SovereignSystem> gameSystems;

    // Sistemas de renderizado (ejecutan en thread de render)
    private final List<VolcanRenderSystem> renderSystems;

    // Métricas de performance
    private long lastExecutionTimeNs;

    // [NEURONA_048 STEP 4] Parallel Execution Infrastructure
    private SystemDependencyGraph dependencyGraph;
    private ParallelSystemExecutor parallelExecutor;
    private boolean parallelMode = false; // Default: sequential (safe)

    public SystemRegistry() {
        // Usamos ArrayList por simplicidad
        // En producción, podríamos usar array fijo para zero-allocation
        this.gameSystems = new ArrayList<>(16);
        this.renderSystems = new ArrayList<>(8);
        this.lastExecutionTimeNs = 0;
        this.dependencyGraph = null;
        this.parallelExecutor = null;
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

        // [NEURONA_048 STEP 4] Usar executor paralelo si está habilitado
        if (parallelMode && parallelExecutor != null) {
            parallelExecutor.execute(state, deltaTime);
            lastExecutionTimeNs = parallelExecutor.getLastExecutionTimeNs();
        } else {
            // Fallback: Ejecución secuencial (modo seguro)
            for (SovereignSystem system : gameSystems) {
                try {
                    system.update(state, deltaTime);
                } catch (Exception e) {
                    System.err.println("[REGISTRY] Error in system " + system.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            long endTime = System.nanoTime();
            lastExecutionTimeNs = endTime - startTime;
        }
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

    /**
     * Construye el grafo de dependencias y habilita ejecución paralela.
     * 
     * DEBE LLAMARSE DESPUÉS de registrar todos los sistemas.
     * 
     * @throws IllegalStateException si hay dependencias circulares
     */
    public void buildDependencyGraph() {
        System.out.println("[REGISTRY] Building dependency graph...");

        dependencyGraph = new SystemDependencyGraph();

        // Agregar todos los sistemas al grafo
        for (SovereignSystem system : gameSystems) {
            String[] deps = system.getDependencies();
            dependencyGraph.addSystem(system, deps);
        }

        // Validar y construir capas de ejecución
        try {
            dependencyGraph.validate();
            dependencyGraph.printGraph();

            // Crear executor paralelo
            parallelExecutor = new ParallelSystemExecutor(dependencyGraph.getExecutionLayers());

            System.out.println("[REGISTRY] Dependency graph built successfully");
            System.out.println("[REGISTRY] Parallel execution ready (" + dependencyGraph.getLayerCount() + " layers)");
        } catch (IllegalStateException e) {
            System.err.println("[REGISTRY] Failed to build dependency graph: " + e.getMessage());
            System.err.println("[REGISTRY] Falling back to sequential execution");
            dependencyGraph = null;
            parallelExecutor = null;
        }
    }

    /**
     * Habilita o deshabilita la ejecución paralela.
     * 
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setParallelMode(boolean enabled) {
        if (enabled && parallelExecutor == null) {
            System.err.println("[REGISTRY] Cannot enable parallel mode: dependency graph not built");
            return;
        }
        this.parallelMode = enabled;
        System.out.println("[REGISTRY] Parallel mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Retorna si el modo paralelo está habilitado.
     * 
     * @return true si está habilitado
     */
    public boolean isParallelMode() {
        return parallelMode;
    }
}
// Creado: 03/01/2026 23:50
// Autor: MarvinDev
// Patrón: Registry Pattern + Strategy Pattern
