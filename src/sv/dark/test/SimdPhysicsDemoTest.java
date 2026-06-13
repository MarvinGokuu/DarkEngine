// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.systems.PhysicsSystem;
import sv.dark.memory.SectorMemoryVault;

/**
 * AAA+ Test for the SIMD Physics Engine.
 */
public class SimdPhysicsDemoTest {

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  DARK ENGINE - AAA+ SIMD PHYSICS BENCHMARK");
        System.out.println("  Target Throughput: < 2.0 ms per 1,000,000 Entities");
        System.out.println("==========================================================");

        int entities = 1_000_000;
        // Total memory needed: 4 arrays of 1_000_000 floats (4MB each) = 16MB.
        // A sector is 64KB. 16MB = 256 sectors.
        SectorMemoryVault vault = new SectorMemoryVault(256);
        PhysicsSystem physics = new PhysicsSystem();

        // Warmup JIT
        System.out.print("Warming up JIT... ");
        for (int i = 0; i < 500; i++) {
            physics.simulateSIMD(vault.getSegment(), entities, 0.016f);
        }
        System.out.println("Done.");

        // Measurement
        System.out.print("Measuring SIMD Physics Execution... ");
        long startNs = System.nanoTime();
        
        physics.simulateSIMD(vault.getSegment(), entities, 0.016f);
        
        long endNs = System.nanoTime();
        double ms = (endNs - startNs) / 1_000_000.0;
        
        System.out.println("Done.");
        System.out.println();
        System.out.println("    -> Entities Processed: " + entities);
        System.out.println(String.format("    -> Execution Time: %.4f ms", ms));
        
        if (ms < 2.0) {
            System.out.println("    [OK] AAA+ Certified (<2.0ms)");
        } else {
            System.out.println("    [WARNING] Physics throughput below AAA+ standards.");
            System.exit(1);
        }
        
        vault.close();
    }
}
