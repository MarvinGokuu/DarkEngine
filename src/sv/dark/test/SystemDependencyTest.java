// Reading Order: 10010101
//  149
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import sv.dark.state.DarkStateLayout;

/**
 * RESPONSIBILITY: Test System B - Depends on SystemExecutionTest.
 * WHY: We need to validate that the DAG dependency resolver properly orders system execution.
 * TECHNIQUE: Implements a GameSystem that explicitly depends on another system and reads its output.
 * GUARANTEES: Executes in Layer 1 after SystemExecutionTest. Can execute in parallel with SystemParallelismTest.
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
public class SystemDependencyTest implements GameSystem {

    @SuppressWarnings("unused") // implement execution counter
    private long executionCount = 0;

    @Override
    public void update(WorldStateFrame state, float deltaTime) {
        executionCount++;

        // Read the value written by SystemExecutionTest
        int valueFromA = state.readInt(DarkStateLayout.PLAYER_X);

        // Simulate work: write to another slot
        state.writeInt(DarkStateLayout.PLAYER_Y, valueFromA * 2);

        // Log disabled to avoid terminal spam
        // if (executionCount % 60 == 0) {
        // System.out.println("[SystemDependencyTest] Executed " + executionCount + "
        // times,
        // computed: " + (valueFromA * 2));
        // }
    }

    @Override
    public String getName() {
        return "SystemDependencyTest";
    }

    @Override
    public String[] getDependencies() {
        return new String[] { "SystemExecutionTest" }; // Depends on SystemExecutionTest
    }
}
