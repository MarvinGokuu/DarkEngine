// Reading Order: 10000110
//  134
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later

package sv.dark.kernel;

import sv.dark.core.DarkLogger;
import sv.dark.core.systems.GameSystem;
import java.util.*;
import sv.dark.core.AAACertified;

/**
 * RESPONSIBILITY: System Dependency Graph for building a deterministic parallel execution graph.
 * WHY: Parallel systems must be executed in the correct order to avoid read/write data races.
 * TECHNIQUE: Implements Kahn's Algorithm (Topological Sort) to build a Directed Acyclic Graph (DAG) of systems.
 * GUARANTEES: Deterministic order, cycle detection, Zero-GC during runtime (Arrays only).
 * 
 * @author Marvin Alexander Flores Canales
 * @version 1.0
 * @since 2026-01-08
 */
@AAACertified(
    date = "2026-06-23",
    maxLatencyNs = 1000,
    minThroughput = 1000,
    alignment = 64,
    lockFree = true,
    offHeap = false,
    notes = "Dependency graph builder - 100% Arrays, No ArrayList"
)
public final class SystemDependencyGraph {

    private final Map<String, GameSystem> systemsByName;
    private final Map<String, Set<String>> dependencies;
    private final Map<String, Set<String>> dependents;

    private GameSystem[][] executionLayersArray;
    private int layerCount = 0;

    private boolean validated = false;

    public SystemDependencyGraph() {
        this.systemsByName = new LinkedHashMap<>(32);
        this.dependencies = new HashMap<>(32);
        this.dependents = new HashMap<>(32);
        this.executionLayersArray = null;
    }

    public void addSystem(GameSystem system, String... dependencyNames) {
        if (system == null) {
            throw new IllegalArgumentException("System cannot be null");
        }

        String systemName = system.getName();
        systemsByName.put(systemName, system);

        Set<String> deps = new HashSet<>(Arrays.asList(dependencyNames));
        dependencies.put(systemName, deps);

        for (String dep : dependencyNames) {
            dependents.computeIfAbsent(dep, k -> new HashSet<>(8)).add(systemName);
        }

        validated = false;
    }

    public void validate() {
        Map<String, Integer> inDegree = new HashMap<>(systemsByName.size());
        for (String system : systemsByName.keySet()) {
            Set<String> deps = dependencies.get(system);
            inDegree.put(system, deps.size());
        }

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            for (String dep : entry.getValue()) {
                if (!systemsByName.containsKey(dep)) {
                    throw new IllegalStateException("Dependency not found: " + dep);
                }
            }
        }

        // Use arrays instead of ArrayList
        GameSystem[][] tempLayers = new GameSystem[16][];
        layerCount = 0;
        
        Set<String> processed = new HashSet<>(systemsByName.size());

        while (processed.size() < systemsByName.size()) {
            GameSystem[] currentLayer = new GameSystem[systemsByName.size()];
            int currentLayerCount = 0;

            for (String systemName : systemsByName.keySet()) {
                if (!processed.contains(systemName) && inDegree.get(systemName) == 0) {
                    currentLayer[currentLayerCount++] = systemsByName.get(systemName);
                    processed.add(systemName);
                }
            }

            if (currentLayerCount == 0) {
                throw new IllegalStateException(
                        "Circular dependency detected! Remaining systems: " +
                                (systemsByName.size() - processed.size()));
            }

            // Trim array to exact size
            GameSystem[] trimmedLayer = new GameSystem[currentLayerCount];
            System.arraycopy(currentLayer, 0, trimmedLayer, 0, currentLayerCount);
            tempLayers[layerCount++] = trimmedLayer;

            for (GameSystem system : trimmedLayer) {
                String systemName = system.getName();
                Set<String> deps = dependents.get(systemName);
                if (deps != null) {
                    for (String dependent : deps) {
                        inDegree.put(dependent, inDegree.get(dependent) - 1);
                    }
                }
            }
        }

        executionLayersArray = new GameSystem[layerCount][];
        System.arraycopy(tempLayers, 0, executionLayersArray, 0, layerCount);

        validated = true;
    }

    public GameSystem[][] getExecutionLayers() {
        if (!validated) {
            throw new IllegalStateException("Graph must be validated before getting execution layers");
        }
        return executionLayersArray;
    }

    public int getLayerCount() {
        if (!validated) {
            throw new IllegalStateException("Graph must be validated first");
        }
        return layerCount;
    }

    public void printGraph() {
        if (executionLayersArray == null || layerCount == 0) {
            DarkLogger.info("GRAPH", "Not validated yet");
            return;
        }
        DarkLogger.info("GRAPH", "Execution Layers: " + layerCount);
        for (int i = 0; i < layerCount; i++) {
            StringBuilder sb = new StringBuilder("[GRAPH] Layer ").append(i).append(": ");
            for (GameSystem system : executionLayersArray[i]) {
                sb.append(system.getName()).append(' ');
            }
            DarkLogger.info("GRAPH", sb.toString());
        }
    }
}
