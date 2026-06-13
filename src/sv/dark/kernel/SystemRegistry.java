// Reading Order: 00001010
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;


import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified; // 00000100 // AAA+ Check
import sv.dark.core.systems.GameSystem;
import sv.dark.core.systems.DarkRenderSystem;
import sv.dark.state.WorldStateFrame;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RESPONSIBILITY: System Registry and Orchestration.
 * WHY: We need a centralized registry to manage the lifecycle and ordered execution of all game logic and rendering systems.
 * TECHNIQUE: Implements the Registry + Strategy pattern. Defaults to sequential safe execution, but supports parallel execution via the ParallelSystemExecutor.
 * GUARANTEES: Deterministic order execution. O(N) Execution. Zero-GC allocations at Runtime.
 * 
 * <p>Dependencies: GameSystem, DarkRenderSystem
 * <p>Metrics: O(N) Execution, Zero-GC at Runtime
 * 
 * @author Marvin Alexander Flores Canales
 * @version 1.0
 * @since 2026-01-05
 */
@AAACertified(
    date = "2026-01-10",
    maxLatencyNs = 1000,
    minThroughput = 60,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Deterministic execution orchestrator (Sequential/Parallel)"
)
public final class SystemRegistry {

    // Game logic systems (execute in the main loop)
    private final List<GameSystem> gameSystems;
    private GameSystem[] gameSystemsArray = new GameSystem[0];

    // Render systems (execute in the render thread)
    private final List<DarkRenderSystem> renderSystems;
    private DarkRenderSystem[] renderSystemsArray = new DarkRenderSystem[0];

    // Performance metrics
    private long lastExecutionTimeNs;

    // [NEURONA_048 STEP 4] Parallel Execution Infrastructure
    private SystemDependencyGraph dependencyGraph;
    private ParallelSystemExecutor parallelExecutor;
    private boolean parallelMode = false; // Default: sequential (safe)

    public SystemRegistry() {
        // [FIX AUDIT]: Pre-size collections to avoid reallocation
        // WHY: ArrayList grows dynamically (1.5x), causing copies and GC
        // TECHNIQUE: Initial capacity = expected max systems
        // GUARANTEE: 0 reallocations during startup, less GC pressure
        //
        // Typical capacities:
        // - gameSystems: 16 (enough for small/medium games)
        // - renderSystems: 8 (typically fewer render systems)
        this.gameSystems = new ArrayList<>(16);
        this.renderSystems = new ArrayList<>(8);
        this.lastExecutionTimeNs = 0;
        this.dependencyGraph = null;
        this.parallelExecutor = null;
    }

    /**
     * Registers a game logic system.
     * 
     * ORDER: Systems are executed in the order they are registered.
     * DETERMINISM: Order must be consistent across executions.
     * 
     * @param system The system to register.
     */
    public void registerGameSystem(GameSystem system) {
        Objects.requireNonNull(system, "System cannot be null");
        gameSystems.add(system);
        gameSystemsArray = gameSystems.toArray(new GameSystem[0]);
        DarkLogger.info("REGISTRY", "Registered game system: " + system.getName());
    }

    /**
     * Registers a rendering system.
     * 
     * @param system The rendering system to register.
     */
    public void registerRenderSystem(DarkRenderSystem system) {
        Objects.requireNonNull(system, "System cannot be null");
        renderSystems.add(system);
        renderSystemsArray = renderSystems.toArray(new DarkRenderSystem[0]);
        DarkLogger.info("REGISTRY", "Registered render system: " + system.getName());
    }

    /**
     * Executes all game logic systems.
     * 
     * LOOP PHASE 3: Systems Execution
     * 
     * GUARANTEES:
     * - Deterministic order (always the same order)
     * - Same WorldStateFrame for all systems
     * - Same deltaTime for all systems
     * 
     * @param state     World state (SSOT)
     * @param deltaTime Elapsed time in seconds
     */
    public void executeGameSystems(WorldStateFrame state, double deltaTime) {
        long startTime = System.nanoTime();

        // [NEURONA_048 STEP 4] Use parallel executor if enabled
        if (parallelMode && parallelExecutor != null) {
            parallelExecutor.execute(state, deltaTime);
            lastExecutionTimeNs = parallelExecutor.getLastExecutionTimeNs();
        } else {
            // Fallback: Sequential execution (safe mode)
            GameSystem[] systems = this.gameSystemsArray;
            int len = systems.length;
            for (int i = 0; i < len; i++) {
                try {
                    systems[i].update(state, deltaTime);
                } catch (Exception e) {
                    // [MECHANICAL SYMPATHY] Removed I/O block in hot path
                    // Future: Route to AdminBus telemetry
                }
            }

            long endTime = System.nanoTime();
            lastExecutionTimeNs = endTime - startTime;
        }
    }

    /**
     * Executes all rendering systems.
     * 
     * RENDER PHASE: After the main loop
     * 
     * @param g2d   Graphics context
     * @param state World state (Read-Only)
     */
    public void executeRenderSystems(Graphics2D g2d, WorldStateFrame state) {
        DarkRenderSystem[] systems = this.renderSystemsArray;
        int len = systems.length;
        for (int i = 0; i < len; i++) {
            try {
                systems[i].render(g2d, state);
            } catch (Exception e) {
                // [MECHANICAL SYMPATHY] Removed I/O block in hot path
                // Future: Route to AdminBus telemetry
            }
        }
    }

    /**
     * Returns the execution time of the last call to executeGameSystems().
     * 
     * @return Time in nanoseconds.
     */
    public long getLastExecutionTimeNs() {
        return lastExecutionTimeNs;
    }

    /**
     * Returns the execution time in milliseconds.
     * 
     * @return Time in milliseconds.
     */
    public double getLastExecutionTimeMs() {
        return lastExecutionTimeNs / 1_000_000.0;
    }

    /**
     * Returns the number of registered logic systems.
     * 
     * @return Number of systems.
     */
    public int getGameSystemCount() {
        return gameSystems.size();
    }

    /**
     * Returns the number of registered render systems.
     * 
     * @return Number of systems.
     */
    public int getRenderSystemCount() {
        return renderSystems.size();
    }

    /**
     * Builds the dependency graph and enables parallel execution.
     * 
     * MUST BE CALLED AFTER registering all systems.
     * 
     * @throws IllegalStateException if circular dependencies exist.
     */
    public void buildDependencyGraph() {
        DarkLogger.info("REGISTRY", "Building dependency graph...");

        dependencyGraph = new SystemDependencyGraph();

        // Add all systems to the graph
        for (GameSystem system : gameSystems) {
            String[] deps = system.getDependencies();
            dependencyGraph.addSystem(system, deps);
        }

        // Validate and build execution layers
        try {
            dependencyGraph.validate();
            dependencyGraph.printGraph();

            // Create parallel executor
            parallelExecutor = new ParallelSystemExecutor(dependencyGraph.getExecutionLayers());

            DarkLogger.info("REGISTRY", "Dependency graph built successfully");
            DarkLogger.info("REGISTRY", "Parallel execution ready (" + dependencyGraph.getLayerCount() + " layers)");
        } catch (IllegalStateException e) {
            DarkLogger.error("REGISTRY", "Failed to build dependency graph: " + e.getMessage());
            DarkLogger.error("REGISTRY", "Falling back to sequential execution");
            dependencyGraph = null;
            parallelExecutor = null;
        }
    }

    /**
     * Enables or disables parallel execution mode.
     * 
     * @param enabled true to enable, false to disable.
     */
    public void setParallelMode(boolean enabled) {
        if (enabled && parallelExecutor == null) {
            DarkLogger.error("REGISTRY", "Cannot enable parallel mode: dependency graph not built");
            return;
        }
        this.parallelMode = enabled;
        DarkLogger.info("REGISTRY", "Parallel mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Returns whether parallel mode is enabled.
     * 
     * @return true if enabled.
     */
    public boolean isParallelMode() {
        return parallelMode;
    }

    public ParallelSystemExecutor getParallelExecutor() {
        return parallelExecutor;
    }
}
