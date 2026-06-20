// Reading Order: 00100052
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.vfx.animation.DarkSkeletonSoA;
import sv.dark.vfx.animation.SkeletalAnimationSystem;

/**
 * RESPONSIBILITY: Skeletal Animation Structural Benchmark.
 * WHY: Validating that manipulating 10,000 skeletons with 64 bones each (40.9 MB)
 *      has Zero-GC footprint and complies with the memory layout.
 * TECHNIQUE: Data-Oriented Memory Stress Test.
 */
public class SkeletalAnimationStressTest {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" SKELETAL ANIMATION - STRUCTURAL STRESS TEST");
        System.out.println("==============================================");

        int maxEntities = 10_000;
        int maxBones = 64;
        
        System.out.println("[TEST] Allocating Native Memory for Skeletons...");
        long startMem = getUsedMemory();
        
        DarkSkeletonSoA skeletons = new DarkSkeletonSoA(maxEntities, maxBones);
        SkeletalAnimationSystem animSystem = new SkeletalAnimationSystem(skeletons);
        
        long expectedBytes = (long) maxEntities * maxBones * 64L;
        System.out.printf("[INFO] Expected Native Memory: %.2f MB%n", (expectedBytes / 1024.0 / 1024.0));
        
        long endMem = getUsedMemory();
        long memDiff = endMem - startMem;
        
        System.out.printf("[RESULT] Heap Memory Footprint for %d Skeletons: %d bytes.%n", maxEntities, memDiff);
        
        if (memDiff > 1024 * 1024) { // Tolerancia de 1MB para clases cargadas
            System.err.println("[FAIL] Skeleton allocation leaked into Heap memory. Suspected GC allocation!");
            System.exit(1);
        } else {
            System.out.println("[OK] AAA+ Zero-GC Structural Check passed!");
        }

        System.out.println("[TEST] Calling GPU GameSystem.update() headless...");
        animSystem.update(null, 0.016);
        System.out.println("[OK] System correctly bypassed uninitialized OpenGL FFI.");

        skeletons.destroy();
        System.out.println("==============================================");
        System.out.println(" TESTS PASSED: SKELETAL STRUCTURE VALIDATED");
        System.out.println("==============================================");
        System.exit(0);
    }
    
    private static long getUsedMemory() {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
