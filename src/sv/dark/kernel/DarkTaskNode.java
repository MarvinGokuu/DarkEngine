// Reading Order: 10000100
//  132
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.kernel;

import sv.dark.core.AAACertified;
import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RESPONSIBILITY: Atomic node in the Directed Acyclic Task Graph (DAG).
 *
 * WHY NODES INSTEAD OF LAYERS:
 * The legacy layer-based executor forces a global barrier after each layer, even if
 * only 1 system in the layer is the bottleneck. For example: if Layer 2 has 5 systems
 * and 4 finish in 0.1ms while 1 takes 2ms, all 4 idle threads wait 1.9ms doing nothing.
 * With DAG nodes, each system independently decrements its dependents' counters the instant
 * it finishes — no global barrier, 100% core utilization.
 *
 * TECHNIQUE: Fine-Grained Dependency Tracking via AtomicInteger countdown.
 *   - pendingDeps: starts at N (number of unfinished predecessors), decrements to 0.
 *   - When pendingDeps reaches 0, this node is ready to execute.
 *   - After execution, decrements successors' pendingDeps atomically.
 *
 * MEMORY LAYOUT: Intentionally flat arrays (no linked lists) to avoid pointer-chasing
 * and to keep hot data within L1/L2 cache lines.
 *
 * THREAD SAFETY:
 * - pendingDeps: AtomicInteger — lock-free decrements from multiple threads.
 * - All other fields are written ONCE during graph compilation (init), read-only at runtime.
 * - state/deltaTime written by dispatcher BEFORE node is published as ready.
 *
 * @author Marvin Alexander Flores Canales
 * @since 4.4.0
 */
@AAACertified(
    date          = "2026-06-28",
    maxLatencyNs  = 100,
    minThroughput = 0,
    alignment     = 64,
    lockFree      = true,
    offHeap       = false,
    notes         = "Lock-Free DAG node. Atomic dependency countdown. Zero-Alloc runtime."
)
public final class DarkTaskNode {

    // -------------------------------------------------------------------------
    // IDENTITY (written once at graph compilation — immutable at runtime)
    // -------------------------------------------------------------------------

    /** The GameSystem this node wraps. Set at compile time. Never null at runtime. */
    final GameSystem system;

    /**
     * Successor nodes (systems that depend on THIS system completing).
     * When this node finishes, it decrements each successor's pendingDeps.
     * Pre-allocated array — size is fixed at graph compile time.
     */
    DarkTaskNode[] successors;

    /**
     * Initial dependency count. Stored separately from pendingDeps so
     * pendingDeps can be reset between frames without extra state.
     */
    final int initialDeps;

    // -------------------------------------------------------------------------
    // FRAME-LOCAL MUTABLE STATE (updated by dispatcher before each execution)
    // -------------------------------------------------------------------------

    /**
     * Atomic countdown of unsatisfied dependencies for this frame.
     * 0 = ready to execute. Starts at initialDeps each frame.
     * WHY AtomicInteger: multiple threads may concrement this simultaneously
     * as different predecessors complete at different times on different cores.
     */
    final AtomicInteger pendingDeps;

    /**
     * Frame state injected by the dispatcher. Written before task is published.
     * Read-only during execution (WorldStateFrame contract: immutable during update).
     */
    volatile WorldStateFrame state;

    /**
     * Frame delta time injected by the dispatcher. Written before task is published.
     */
    volatile float deltaTime;

    // -------------------------------------------------------------------------
    // CONSTRUCTOR
    // -------------------------------------------------------------------------

    /**
     * @param system      The GameSystem to execute. Must not be null.
     * @param initialDeps Number of systems that must complete before this one runs.
     *                    0 = no dependencies, can be dispatched immediately.
     */
    DarkTaskNode(GameSystem system, int initialDeps) {
        this.system      = system;
        this.initialDeps = initialDeps;
        this.pendingDeps = new AtomicInteger(initialDeps);
        this.successors  = new DarkTaskNode[0]; // Populated by DarkTaskGraph.compile()
    }

    /**
     * Resets the pending dependency counter for the next frame.
     * Called by the dispatcher at the start of each frame.
     * // [RENDER_THREAD_ONLY] — called from single dispatcher thread before workers see the node.
     */
    void resetForFrame() {
        pendingDeps.set(initialDeps);
    }

    /**
     * Signals that one predecessor has completed.
     * Returns true if this node is now ready to execute (all deps satisfied).
     * // [THREAD_SAFE] — may be called from any worker thread.
     */
    boolean decrementAndCheckReady() {
        return pendingDeps.decrementAndGet() == 0;
    }

    @Override
    public String toString() {
        return "DarkTaskNode[" + system.getName() + " deps=" + initialDeps + "]";
    }
}
