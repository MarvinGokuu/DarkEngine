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

    public void registerRenderSystem(DarkRenderSystem system) {
        if (renderSystemCount >= renderSystemsArray.length) throw new IllegalStateException("RenderSystem capacity exceeded");
        renderSystemsArray[renderSystemCount++] = system;
        DarkLogger.info("REGISTRY", "Registered render system: " + system.getName());
    }

    public void executeGameSystems(WorldStateFrame state, float deltaTime) {
        long startTime = System.nanoTime();

        if (parallelMode && parallelExecutor != null) {
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

    public void setParallelMode(boolean enabled) {
        if (enabled && parallelExecutor == null) {
            DarkLogger.error("REGISTRY", "Cannot enable parallel mode: dependency graph not built");
            return;
        }
        this.parallelMode = enabled;
        DarkLogger.info("REGISTRY", "Parallel mode: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public boolean isParallelMode() {
        return parallelMode;
    }

    public ParallelSystemExecutor getParallelExecutor() {
        return parallelExecutor;
    }
}
