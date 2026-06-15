// Reading Order: 01100000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.core.DarkLogger;
import sv.dark.scene.DarkTransformSoA;
import sv.dark.scene.DarkKinematicsSystem;

public class SystemSIMDKinematicsTest {
    
    private static final int ENTITY_COUNT = 1_000_000;

    public static void main(String[] args) {
        DarkLogger.info("TEST", "[16/16] Running SIMD Kinematics Throughput (" + ENTITY_COUNT + " entities)");
        
        DarkTransformSoA soa = new DarkTransformSoA(ENTITY_COUNT);
        
        // Inicializar entidades aleatoriamente
        for(int i = 0; i < ENTITY_COUNT; i++) {
            soa.setEntity(i, 0.0, 0.0, 10.5f, -5.2f);
        }
        
        // Calentamiento JIT
        for(int i = 0; i < 50; i++) {
            DarkKinematicsSystem.update(soa, 0.016f, 0.0, 0.0);
        }
        
        // Medición
        long start = System.nanoTime();
        DarkKinematicsSystem.update(soa, 0.016f, 0.0, 0.0);
        long end = System.nanoTime();
        
        double durationMs = (end - start) / 1_000_000.0;
        
        DarkLogger.info("TEST", String.format("SIMD Kinematics processed %d entities in %.4f ms", ENTITY_COUNT, durationMs));
        
        soa.destroy();
        
        if (durationMs > 4.0) { // Tolerancia máxima 4ms para ser AAA+ (64-bits reduce carriles a la mitad)
            throw new RuntimeException("Kinematics latency exceeded 4.0 ms: " + durationMs + " ms");
        }
        
        DarkLogger.info("TEST", "[OK] AAA+ Kinematics Throughput passed.");
    }
}
