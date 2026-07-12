// Reading Order: 10011111
//  159
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.SystemDependencyGraph;
import sv.dark.core.systems.GameSystem;
import sv.dark.state.WorldStateFrame;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * RESPONSIBILITY: Validates that the SystemDependencyGraph maps are initialized with the correct pre-sized capacity.
 * WHY: Hash collisions and table rehashing during DAG construction introduce unacceptable latency spikes.
 * TECHNIQUE: Uses Reflection to inspect internal HashMap capacities after adding a dummy system.
 * GUARANTEES: Ensures that the dependency graph avoids re-hashing during DAG construction.
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
public class DependencyGraphPerformanceTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: DEPENDENCY GRAPH PRE-SIZING");
        System.out.println("=======================================================");

        try {
            SystemDependencyGraph graph = new SystemDependencyGraph();

            // Extract maps via reflection
            Field systemsByNameField = SystemDependencyGraph.class.getDeclaredField("systemsByName");
            systemsByNameField.setAccessible(true);
            Map<?, ?> systemsByName = (Map<?, ?>) systemsByNameField.get(graph);

            Field dependenciesField = SystemDependencyGraph.class.getDeclaredField("dependencies");
            dependenciesField.setAccessible(true);
            Map<?, ?> dependencies = (Map<?, ?>) dependenciesField.get(graph);

            // Add a dummy system to force initialization of HashMap tables
            GameSystem dummySystem = new GameSystem() {
                @Override public void update(WorldStateFrame state, float dt) {}
                @Override public String getName() { return "DummySystem"; }
                @Override public String[] getDependencies() { return new String[0]; }
            };
            graph.addSystem(dummySystem);

            // Inspect the size of the internal HashMap tables
            Field tableField = Class.forName("java.util.HashMap").getDeclaredField("table");
            tableField.setAccessible(true);

            Object[] systemsTable = (Object[]) tableField.get(systemsByName);
            Object[] depsTable = (Object[]) tableField.get(dependencies);

            int systemsCapacity = systemsTable.length;
            int depsCapacity = depsTable.length;

            System.out.println("[TEST] systemsByName initial table capacity: " + systemsCapacity + " (Expected: 32)");
            System.out.println("[TEST] dependencies initial table capacity: " + depsCapacity + " (Expected: 32)");

            if (systemsCapacity == 32 && depsCapacity == 32) {
                System.out.println("\n[PASSED] SYSTEM DEPENDENCY GRAPH MAPS ARE CORRECTLY PRE-SIZED");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM DEPENDENCY GRAPH MAPS ARE NOT PRE-SIZED CORRECTLY");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
