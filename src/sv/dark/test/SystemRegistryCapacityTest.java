// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.SystemRegistry;
import java.lang.reflect.Field;
import java.util.List;

/**
 * RESPONSIBILITY: Validates that the SystemRegistry collections are initialized with the correct pre-sized capacity.
 * WHY: Dynamic array resizing (ArrayList) causes allocations and array copies, which degrade startup and runtime performance.
 * TECHNIQUE: Uses reflection to access internal 'elementData' array of ArrayLists and asserts their initial sizes.
 * GUARANTEES: Collections are correctly pre-sized to avoid reallocations in the hot-path.
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
public class SystemRegistryCapacityTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: SYSTEM REGISTRY PRE-SIZING");
        System.out.println("=======================================================");

        try {
            SystemRegistry registry = new SystemRegistry();

            // Extract gameSystems
            Field gameSystemsField = SystemRegistry.class.getDeclaredField("gameSystems");
            gameSystemsField.setAccessible(true);
            List<?> gameSystems = (List<?>) gameSystemsField.get(registry);

            // Extract renderSystems
            Field renderSystemsField = SystemRegistry.class.getDeclaredField("renderSystems");
            renderSystemsField.setAccessible(true);
            List<?> renderSystems = (List<?>) renderSystemsField.get(registry);

            // Inspect the capacity of the ArrayList through the private 'elementData' field
            Field elementDataField = Class.forName("java.util.ArrayList").getDeclaredField("elementData");
            elementDataField.setAccessible(true);

            Object[] gameSystemsBackingArray = (Object[]) elementDataField.get(gameSystems);
            Object[] renderSystemsBackingArray = (Object[]) elementDataField.get(renderSystems);

            int gameCapacity = gameSystemsBackingArray.length;
            int renderCapacity = renderSystemsBackingArray.length;

            System.out.println("[TEST] gameSystems collection initial capacity: " + gameCapacity + " (Expected: 16)");
            System.out.println("[TEST] renderSystems collection initial capacity: " + renderCapacity + " (Expected: 8)");

            if (gameCapacity == 16 && renderCapacity == 8) {
                System.out.println("\n[PASSED] SYSTEM REGISTRY COLLECTIONS ARE CORRECTLY PRE-SIZED");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM REGISTRY COLLECTIONS ARE NOT PRE-SIZED CORRECTLY");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
