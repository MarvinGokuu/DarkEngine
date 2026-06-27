// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.AAACertified;

import sv.dark.kernel.SystemRegistry;
import java.lang.reflect.Field;

/**
 * RESPONSIBILITY: Validates that the SystemRegistry uses fixed arrays with correct capacity.
 * WHY: Dynamic array resizing causes allocations and array copies, degrading runtime performance.
 * TECHNIQUE: Uses reflection to access internal arrays and asserts their exact sizes.
 * GUARANTEES: Collections are correctly pre-sized to avoid reallocations.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(
    date = "2026-06-23",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Validates 100% Zero-Garbage arrays in SystemRegistry"
)
public class SystemRegistryCapacityTest {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  AAA+ CERTIFICATION: SYSTEM REGISTRY ZERO-GARBAGE ARRAYS");
        System.out.println("=======================================================");

        try {
            SystemRegistry registry = new SystemRegistry();

            // Extract gameSystemsArray
            Field gameSystemsField = SystemRegistry.class.getDeclaredField("gameSystemsArray");
            gameSystemsField.setAccessible(true);
            Object[] gameSystemsArray = (Object[]) gameSystemsField.get(registry);

            // Extract renderSystemsArray
            Field renderSystemsField = SystemRegistry.class.getDeclaredField("renderSystemsArray");
            renderSystemsField.setAccessible(true);
            Object[] renderSystemsArray = (Object[]) renderSystemsField.get(registry);

            int gameCapacity = gameSystemsArray.length;
            int renderCapacity = renderSystemsArray.length;

            System.out.println("[TEST] gameSystems array capacity: " + gameCapacity + " (Expected: 64)");
            System.out.println("[TEST] renderSystems array capacity: " + renderCapacity + " (Expected: 32)");

            if (gameCapacity == 64 && renderCapacity == 32) {
                System.out.println("\n[PASSED] SYSTEM REGISTRY ARRAYS ARE CORRECTLY SIZED");
                System.exit(0);
            } else {
                System.err.println("\n[FAILED] SYSTEM REGISTRY ARRAYS ARE NOT SIZED CORRECTLY");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
