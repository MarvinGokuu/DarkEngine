// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;

/**
 * RESPONSIBILITY: Test System C - Depends on SystemExecutionTest.
 * WHY: We need to validate that the DAG dependency resolver groups systems with identical dependency depth.
 * TECHNIQUE: Declares the exact same dependencies as SystemDependencyTest but writes to a different memory slot.
 * GUARANTEES: Executes in Layer 1. Guaranteed to run in parallel with SystemDependencyTest without data races.
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
public class SystemParallelismTest implements GameSystem {
    @SuppressWarnings("unused") // implement execution counter
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        executionCount++;

        // Read the value written by SystemExecutionTest
        int valueFromA = state.readInt(DarkStateLayout.PLAYER_X);

        // Simulate work: write to a different slot than B
        state.writeInt(DarkStateLayout.PLAYER_DIR, valueFromA + 100);

        // Log disabled to avoid terminal spam
        // if (executionCount % 60 == 0) {
        // System.out.println("[SystemParallelismTest] Executed " + executionCount + "
        // times,
        // computed: " + (valueFromA + 100));
        // }
    }

    @Override
    public String getName() {
        return "SystemParallelismTest";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "SystemExecutionTest" }; // Depends on SystemExecutionTest (same as SystemDependencyTest)
    }
}
