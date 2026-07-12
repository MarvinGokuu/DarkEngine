// Reading Order: 10100010
//  162
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.vfx.DarkParticleEmitterSoA;
import sv.dark.vfx.GPUParticleSystem;
import java.lang.foreign.ValueLayout;

/**
 * RESPONSIBILITY: GPU Particle System Structural Benchmark.
 * WHY: Validating that manipulating the Emitter Off-Heap memory has Zero-GC footprint.
 * TECHNIQUE: Data-Oriented Memory Stress Test.
 */
public class GPUParticleStressTest {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" GPU PARTICLE SYSTEM - STRUCTURAL STRESS TEST");
        System.out.println("==============================================");

        int maxEmitters = 512;
        int targetParticles = 1_000_000;
        
        System.out.println("[TEST] Allocating Native Memory for Emitters...");
        long startMem = getUsedMemory();
        
        DarkParticleEmitterSoA emitters = new DarkParticleEmitterSoA(maxEmitters);
        GPUParticleSystem gpuSystem = new GPUParticleSystem(emitters);
        
        System.out.println("[TEST] Simulating Emitter configurations...");
        for (int i = 0; i < maxEmitters; i++) {
            emitters.posX.set(ValueLayout.JAVA_FLOAT, i * 4L, (float)(Math.random() * 1000));
            emitters.posY.set(ValueLayout.JAVA_FLOAT, i * 4L, (float)(Math.random() * 1000));
            emitters.emissionRate.set(ValueLayout.JAVA_FLOAT, i * 4L, 2000.0f); // 2000 particles/sec
        }

        long endMem = getUsedMemory();
        long memDiff = endMem - startMem;
        
        System.out.printf("[RESULT] Heap Memory Footprint for %d Emitters: %d bytes.%n", maxEmitters, memDiff);
        
        if (memDiff > 1024 * 1024) { // Allow 1MB jitter max, ideal is < 100KB
            System.err.println("[FAIL] Emitter allocation leaked into Heap memory. Suspected GC allocation!");
            System.exit(1);
        } else {
            System.out.println("[OK] AAA+ Zero-GC Structural Check passed!");
        }

        System.out.println("[TEST] Calling GPU GameSystem.update() headless...");
        gpuSystem.update(null, 0.016f);
        System.out.println("[OK] System correctly bypassed uninitialized OpenGL FFI.");

        emitters.destroy();
        System.out.println("==============================================");
        System.out.println(" TESTS PASSED: GPU PARTICLE STRUCTURE VALIDATED");
        System.out.println("==============================================");
        System.exit(0);
    }
    
    private static long getUsedMemory() {
        System.gc(); // Force GC for accurate reading
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
}
