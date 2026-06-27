// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import sv.dark.core.AAACertified;

import sv.dark.state.WorldStateFrame;

/**
 * RESPONSIBILITY: Define the base interface for all logical engine systems.
 * WHY: To enforce the Strategy pattern and the principle of Immutability during execution.
 * TECHNIQUE: Method injection of the WorldStateFrame and deterministic deltaTime.
 * GUARANTEES: Zero-allocation interface and deterministic state updates.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public interface GameSystem {

    /**
     * Executes the system logic for an engine tick.
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * EXECUTION CONTRACT
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * PRECONDITIONS (what the Kernel guarantees):
     * 1. state != null
     * 2. state is immutable during this call
     * 3. deltaTime > 0 and deltaTime < TICK_BUDGET
     * 4. No other concurrent calls to this system
     * 
     * POSTCONDITIONS (what the system must guarantee):
     * 1. Only modifies data via state.writeXXX()
     * 2. Does not create objects on the Heap
     * 3. Returns in deterministic time (< 1ms typically)
     * 4. Same input produces same output (bit-perfect)
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * CONCURRENCY
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * MODEL: Single-threaded per system
     * - The Kernel can execute DIFFERENT systems in parallel
     * - The Kernel NEVER executes the SAME system concurrently
     * - No locks required inside the system
     * 
     * PARALLEL EXECUTION EXAMPLE:
     * Thread 1: MovementSystem.update(frame, dt)
     * Thread 2: SpriteSystem.update(frame, dt) // OK: different systems
     * 
     * FORBIDDEN EXAMPLE:
     * Thread 1: MovementSystem.update(frame1, dt)
     * Thread 2: MovementSystem.update(frame2, dt) // NEVER: same system
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * DETERMINISM
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * DEFINITION: For the same input state and deltaTime, the system
     * must produce exactly the same output state.
     * 
     * MATHEMATICALLY:
     * update(state, dt) = result
     * update(state, dt) = result // Always the same result
     * 
     * APPLICATIONS:
     * - REPLAY: Record inputs and reproduce exact game
     * - NETCODE: Synchronize clients with only inputs (no full state)
     * - DEBUGGING: Reproduce bugs consistently
     * - TESTING: Deterministic and reproducible tests
     * 
     * HOW TO GUARANTEE IT:
     * [OK] Use only data from WorldStateFrame
     * [OK] Use provided deltaTime (not System.currentTimeMillis())
     * [OK] Use frame seeds for random (not Math.random())
     * [OK] Avoid non-deterministic floating-point (use double, not float)
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * PERFORMANCE
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * TARGET: < 1ms per system for 60 FPS
     * 
     * TIME BUDGET (60 FPS = 16.6ms per frame):
     * - Input Processing: 1ms
     * - Bus Processing: 0.5ms
     * - Systems Execution: 10ms (distributed among N systems)
     * - State Hashing: 0.5ms
     * - Rendering: 4ms
     * - Buffer: 0.6ms
     * 
     * OPTIMIZATION TECHNIQUES:
     * 1. Cache Locality: Sequential memory access
     * 2. SIMD: Vectorized operations when possible
     * 3. Branch Prediction: Minimize if/else in loops
     * 4. Prefetching: CPU can predict accesses
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * @param state     Immutable snapshot of the world state (Single Source of
     *                  Truth).
     *                  This frame contains ALL game data in Off-Heap memory.
     * 
     *                  ACCESS:
     *                  - Read: state.readDouble(offset), state.readInt(offset)
     *                  - Write: state.writeDouble(offset, value)
     * 
     *                  IMMUTABILITY:
     *                  - The frame does NOT change during update()
     *                  - Writes go to a buffer that is committed later
     *                  - This guarantees all systems see the same state
     * 
     * @param deltaTime Time elapsed since the last tick in seconds.
     * 
     *                  TYPICAL RANGE: 0.016 seconds (60 FPS) to 0.033 seconds (30
     *                  FPS)
     * 
     *                  USAGE:
     *                  - Physics: position += velocity * deltaTime
     *                  - Animation: frame += frameRate * deltaTime
     *                  - Timers: timer -= deltaTime
     * 
     *                  DETERMINISM:
     *                  - The Kernel guarantees deltaTime is consistent
     *                  - For the same tick, all systems see the same
     *                  deltaTime
     *                  - Do not use System.nanoTime() or similar
     * 
     * @throws RuntimeException Only in cases of unrecoverable errors
     *                          (memory corruption, contract violation)
     * 
     * @see WorldStateFrame For memory access details
     * @see EntityLayout For entity offsets
     * @see DarkStateLayout For kernel state offsets
     */
    void update(WorldStateFrame state, float deltaTime);

    /**
     * Returns the system name for debugging and telemetry.
     * 
     * DEFAULT IMPLEMENTATION: Uses the class name.
     * 
     * OVERRIDE: Only if you need a custom name.
     * 
     * EXAMPLE:
     * 
     * @Override
     *           public String getName() {
     *           return "PhysicsSystem-v2";
     *           }
     * 
     * @return System name (not null, not empty)
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the names of systems that this system depends on.
     * 
     * PURPOSE: Build the dependency graph for parallel execution.
     * 
     * SEMANTICS:
     * - If this system depends on "PhysicsSystem", it must return {"PhysicsSystem"}
     * - The Kernel guarantees that dependencies are executed BEFORE
     * - Systems without dependencies can be executed in parallel
     * 
     * DEFAULT IMPLEMENTATION: No dependencies (can be executed first)
     * 
     * OVERRIDE: Only if the system needs others to execute first
     * 
     * EXAMPLE:
     * 
     * @Override
     *           public String[] getDependencies() {
     *           return new String[]{"PhysicsSystem", "InputSystem"};
     *           }
     * 
     *           IMPORTANT:
     *           - Do not create circular dependencies (A->B->A)
     *           - The Kernel detects cycles and fails fast
     *           - Minimize dependencies to maximize parallelism
     * 
     * @return Array of system names (can be empty, not null)
     */
    default String[] getDependencies() {
        return new String[0]; // No dependencies by default
    }
}
// Created: 03/01/2026 23:35
// Role: Software Architect applying Strategy Pattern + SOLID
// Principles: ISP, OCP, SSOT, Determinism, Immutability
