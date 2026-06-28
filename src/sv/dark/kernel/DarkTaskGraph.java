// Reading Order: 00010000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.kernel;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.core.systems.GameSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * RESPONSIBILITY: Compile a SystemDependencyGraph into an executable DarkTaskNode[].
 *
 * WHY A SEPARATE COMPILER PHASE:
 * The SystemDependencyGraph uses HashMaps and Sets — appropriate for the boot-time
 * compilation step (fast to build, easy to query), but catastrophically bad for
 * per-frame execution (pointer-chasing, GC pressure, cache misses).
 * DarkTaskGraph converts the rich HashSet graph into a flat, cache-friendly
 * DarkTaskNode[] array where all relationships are encoded as direct object references.
 * The compiled form is immutable at runtime — zero allocations per frame.
 *
 * COMPILATION STEPS (boot time only, not hot-path):
 *   1. Read the validated execution layers from SystemDependencyGraph.
 *   2. Create one DarkTaskNode per system.
 *   3. For each node, count how many predecessors it has (initialDeps).
 *   4. For each node, build its successors[] array by scanning all other nodes
 *      that declare this node's system as a dependency.
 *   5. Store the "root nodes" (initialDeps == 0) separately — these are
 *      dispatched immediately at the start of each frame.
 *
 * RUNTIME CONTRACT:
 * - allNodes[] is read-only at runtime.
 * - rootNodes[] is read-only at runtime.
 * - No HashMap, no HashSet, no iterator — only array loops.
 *
 * GUARANTEES:
 * - Single compile() call during boot — O(N²) is acceptable here.
 * - Per-frame dispatch is O(N) — only array loops on pre-compiled state.
 * - Zero heap allocations after compile().
 *
 * @author Marvin Alexander Flores Canales
 * @since 4.4.0
 */
@AAACertified(
    date          = "2026-06-28",
    maxLatencyNs  = 0,     // No latency constraint — boot-time only
    minThroughput = 0,
    alignment     = 64,
    lockFree      = true,
    offHeap       = false,
    notes         = "Boot-time DAG compiler. Zero-Alloc at runtime after compile()."
)
public final class DarkTaskGraph {

    /** All nodes in the compiled graph. Indexed for O(1) lookup. */
    private DarkTaskNode[] allNodes;

    /** Nodes with zero dependencies — dispatched immediately at frame start. */
    private DarkTaskNode[] rootNodes;

    /** Total number of nodes (systems). */
    private int nodeCount;

    /** Number of root nodes. */
    private int rootCount;

    private boolean compiled = false;

    // -------------------------------------------------------------------------
    // COMPILATION (boot-time — called ONCE from EngineKernel.init())
    // -------------------------------------------------------------------------

    /**
     * Compiles the validated SystemDependencyGraph into a flat DarkTaskNode[].
     * Must be called ONCE at boot time. Not thread-safe.
     *
     * @param graph A validated SystemDependencyGraph (graph.validate() must have been called).
     */
    public void compile(SystemDependencyGraph graph) {
        if (compiled) {
            DarkLogger.info("TASKGRAPH", "compile() called more than once — skipped.");
            return;
        }

        GameSystem[][] layers = graph.getExecutionLayers();
        int layerCount = graph.getLayerCount();

        // 1. Count total systems
        nodeCount = 0;
        for (int i = 0; i < layerCount; i++) {
            nodeCount += layers[i].length;
        }

        DarkLogger.info("TASKGRAPH", "Compiling DAG for " + nodeCount + " systems across "
                + layerCount + " layers...");

        // 2. Create one node per system
        allNodes = new DarkTaskNode[nodeCount];
        int idx = 0;
        for (int i = 0; i < layerCount; i++) {
            for (GameSystem system : layers[i]) {
                // Layer index = dependency depth = initialDeps count.
                // Layer 0 systems have 0 deps. Layer 1 systems depend on ≥1 layer-0 system, etc.
                // We use the actual dependency count from the system's getDependencies() for precision.
                int depCount = system.getDependencies().length;
                allNodes[idx++] = new DarkTaskNode(system, depCount);
            }
        }

        // 3. Build a fast name→node lookup map (boot-time only, discarded after compile)
        Map<String, DarkTaskNode> nodeByName = new HashMap<>(nodeCount * 2);
        for (DarkTaskNode node : allNodes) {
            nodeByName.put(node.system.getName(), node);
        }

        // 4. Build successors[] arrays for each node.
        // For node A: A.successors = all nodes B where B declares A as a dependency.
        // We do this in two passes: count pass → allocate → fill pass.
        for (DarkTaskNode node : allNodes) {
            String nodeName = node.system.getName();
            // Count how many successors this node has
            int successorCount = 0;
            for (DarkTaskNode candidate : allNodes) {
                for (String dep : candidate.system.getDependencies()) {
                    if (dep.equals(nodeName)) {
                        successorCount++;
                        break;
                    }
                }
            }
            // Allocate exact-size successors array
            node.successors = new DarkTaskNode[successorCount];
            // Fill it
            int si = 0;
            for (DarkTaskNode candidate : allNodes) {
                for (String dep : candidate.system.getDependencies()) {
                    if (dep.equals(nodeName)) {
                        node.successors[si++] = candidate;
                        break;
                    }
                }
            }
        }

        // 5. Collect root nodes (zero initial dependencies)
        rootCount = 0;
        for (DarkTaskNode node : allNodes) {
            if (node.initialDeps == 0) rootCount++;
        }
        rootNodes = new DarkTaskNode[rootCount];
        int ri = 0;
        for (DarkTaskNode node : allNodes) {
            if (node.initialDeps == 0) rootNodes[ri++] = node;
        }

        compiled = true;
        DarkLogger.info("TASKGRAPH", "DAG compiled: " + nodeCount + " nodes, "
                + rootCount + " root nodes (immediate dispatch). "
                + "Successors wired: all systems will notify dependents on completion.");

        // Log the graph topology for diagnostics
        printTopology();
    }

    // -------------------------------------------------------------------------
    // RUNTIME ACCESSORS (called per-frame by DarkTaskDispatcher — read-only)
    // -------------------------------------------------------------------------

    /** Returns all compiled nodes. Read-only. */
    DarkTaskNode[] getAllNodes() { return allNodes; }

    /** Returns all root nodes (zero-dependency systems). Read-only. */
    DarkTaskNode[] getRootNodes() { return rootNodes; }

    /** Returns the total number of nodes. */
    int getNodeCount() { return nodeCount; }

    /** Returns the number of root nodes. */
    int getRootCount() { return rootCount; }

    boolean isCompiled() { return compiled; }

    // -------------------------------------------------------------------------
    // DIAGNOSTICS
    // -------------------------------------------------------------------------

    private void printTopology() {
        for (DarkTaskNode node : allNodes) {
            StringBuilder sb = new StringBuilder("[TASKGRAPH]   ")
                .append(node.system.getName())
                .append(" (deps=").append(node.initialDeps).append(")")
                .append(" → [");
            for (int i = 0; i < node.successors.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(node.successors[i].system.getName());
            }
            sb.append("]");
            DarkLogger.info("TASKGRAPH", sb.toString());
        }
    }
}
