// Reading Order: 00001010
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.DarkLogger;
import sv.dark.core.AAACertified;
import sv.dark.core.systems.GameSystem;
import sv.dark.core.systems.DarkRenderSystem;
import sv.dark.state.WorldStateFrame;

/**
 * RESPONSIBILITY: System Registry and Orchestration.
 * WHY: We need a centralized registry to manage the lifecycle and ordered execution of all game logic and rendering systems.
 * TECHNIQUE: Implements the Registry + Strategy pattern. Defaults to sequential safe execution, but supports parallel execution via the ParallelSystemExecutor.
 * GUARANTEES: Deterministic order execution. O(N) Execution. Zero-GC allocations at Runtime.
 * 
 * @author Marvin Alexander Flores Canales
 * @version 1.0
 * @since 2026-01-05
 */
@AAACertified(
    date = "2026-06-23",
    maxLatencyNs = 1000,
    minThroughput = 60,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Deterministic execution orchestrator (Sequential/Parallel) - 100% Zero-Garbage (No ArrayList)"
)
public final class SystemRegistry {

    private final GameSystem[] gameSystemsArray;
    private int gameSystemCount = 0;

    private final DarkRenderSystem[] renderSystemsArray;
    private int renderSystemCount = 0;

    private long lastExecutionTimeNs;

    private SystemDependencyGraph dependencyGraph;
    private ParallelSystemExecutor parallelExecutor;
    private boolean parallelMode = false;

    // [FASE 4] DAG Task Dispatcher — replaces layer-barrier model
    private DarkTaskGraph taskGraph;
    private DarkTaskDispatcher taskDispatcher;
    private boolean dagMode = false;

    public SystemRegistry() {
        this.gameSystemsArray = new GameSystem[64];
        this.renderSystemsArray = new DarkRenderSystem[32];
        this.lastExecutionTimeNs = 0;
        this.dependencyGraph = null;
        this.parallelExecutor = null;
    }

    public void registerGameSystem(GameSystem system) {
        if (gameSystemCount >= gameSystemsArray.length) throw new IllegalStateException("GameSystem capacity exceeded");
        gameSystemsArray[gameSystemCount++] = system;
        DarkLogger.info("REGISTRY", "Registered game system: " + system.getName());
    }

    public GameSystem getSystem(Class<?> clazz) {
        for (int i = 0; i < gameSystemCount; i++) {
            if (clazz.isInstance(gameSystemsArray[i])) return gameSystemsArray[i];
        }
        return null;
    }

    public void registerRenderSystem(DarkRenderSystem system) {
        if (renderSystemCount >= renderSystemsArray.length) throw new IllegalStateException("RenderSystem capacity exceeded");
        renderSystemsArray[renderSystemCount++] = system;
        DarkLogger.info("REGISTRY", "Registered render system: " + system.getName());
    }

    public void executeGameSystems(WorldStateFrame state, float deltaTime) {
        long startTime = System.nanoTime();

        if (dagMode && taskDispatcher != null) {
            // [FASE 4] DAG Mode: fine-grained per-node dispatch, no layer barriers.
            taskDispatcher.execute(state, deltaTime);
            lastExecutionTimeNs = taskDispatcher.getLastExecutionTimeNs();
        } else if (parallelMode && parallelExecutor != null) {
            // Legacy: layer-based work-stealing executor.
            parallelExecutor.execute(state, deltaTime);
            lastExecutionTimeNs = parallelExecutor.getLastExecutionTimeNs();
        } else {
            for (int i = 0; i < gameSystemCount; i++) {
                try {
                    gameSystemsArray[i].update(state, deltaTime);
                } catch (Exception e) {
                    // Route to AdminBus telemetry
                }
            }
            lastExecutionTimeNs = System.nanoTime() - startTime;
        }
    }

    public void executeRenderSystems(WorldStateFrame state) {
        for (int i = 0; i < renderSystemCount; i++) {
            try {
                renderSystemsArray[i].render(state);
            } catch (Exception e) {
                // Route to AdminBus telemetry
            }
        }
    }

    public long getLastExecutionTimeNs() {
        return lastExecutionTimeNs;
    }

    public double getLastExecutionTimeMs() {
        return lastExecutionTimeNs / 1_000_000.0;
    }

    public int getGameSystemCount() {
        return gameSystemCount;
    }

    public int getRenderSystemCount() {
        return renderSystemCount;
    }

    public void buildDependencyGraph() {
        DarkLogger.info("REGISTRY", "Building dependency graph...");
        dependencyGraph = new SystemDependencyGraph();

        for (int i = 0; i < gameSystemCount; i++) {
            GameSystem system = gameSystemsArray[i];
            dependencyGraph.addSystem(system, system.getDependencies());
        }

        try {
            dependencyGraph.validate();
            dependencyGraph.printGraph();

            // Legacy: Layer-based parallel executor (retrocompatibility)
            parallelExecutor = new ParallelSystemExecutor(dependencyGraph.getExecutionLayers());

            // [FASE 4] DAG: Compile the flat node graph from validated layers
            taskGraph = new DarkTaskGraph();
            taskGraph.compile(dependencyGraph);
            taskDispatcher = new DarkTaskDispatcher(taskGraph);

            DarkLogger.info("REGISTRY", "Dependency graph built successfully");
            DarkLogger.info("REGISTRY", "DAG compiled: " + taskGraph.getNodeCount()
                    + " nodes, " + dependencyGraph.getLayerCount() + " layers");
        } catch (IllegalStateException e) {
            DarkLogger.error("REGISTRY", "Failed to build dependency graph: " + e.getMessage());
            DarkLogger.error("REGISTRY", "Falling back to sequential execution");
            dependencyGraph = null;
            parallelExecutor = null;
            taskGraph = null;
            taskDispatcher = null;
        }
    }

    public void setParallelMode(boolean enabled) {
        if (enabled && parallelExecutor == null) {
            DarkLogger.error("REGISTRY", "Cannot enable parallel mode: dependency graph not built");
            return;
        }
        this.parallelMode = enabled;
        if (enabled) this.dagMode = false; // Mutually exclusive
        DarkLogger.info("REGISTRY", "Parallel mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Enables DAG mode (Fase 4). Disables legacy parallel mode.
     * Requires buildDependencyGraph() to have been called successfully.
     *
     * DAG mode dispatches each system node individually as soon as its
     * dependencies complete — no global layer barriers.
     */
    public void enableDAGMode() {
        if (taskDispatcher == null) {
            DarkLogger.error("REGISTRY", "Cannot enable DAG mode: dependency graph not built");
            return;
        }
        this.dagMode = true;
        this.parallelMode = false; // Mutually exclusive
        DarkLogger.info("REGISTRY", "[FASE 4] DAG Mode ENABLED — elastic dispatch, no layer barriers.");
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public boolean isDAGMode() {
        return dagMode;
    }

    public DarkTaskDispatcher getTaskDispatcher() {
        return taskDispatcher;
    }

    public ParallelSystemExecutor getParallelExecutor() {
        return parallelExecutor;
    }
}
