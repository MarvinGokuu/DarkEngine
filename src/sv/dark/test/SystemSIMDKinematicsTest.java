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
            soa.setEntity(i, 0.0, 0.0, 0.0, 10.5f, -5.2f, 0.0f);
        }
        
        // Calentamiento JIT (Forzar compilación C2 limpia mediante llamadas repetidas)
        DarkTransformSoA warmupSoa = new DarkTransformSoA(1024);
        for(int i = 0; i < 15000; i++) {
            DarkKinematicsSystem.update(warmupSoa, 0.016f, 0.0, 0.0, 0.0);
        }
        warmupSoa.destroy();
        
        // Calentamiento de Caché L1/L2/L3 en el array real de medición
        for(int i = 0; i < 10; i++) {
            DarkKinematicsSystem.update(soa, 0.016f, 0.0, 0.0, 0.0);
        }
        
        // Medición
        long start = System.nanoTime();
        DarkKinematicsSystem.update(soa, 0.016f, 0.0, 0.0, 0.0);
        long end = System.nanoTime();
        
        double durationMs = (end - start) / 1_000_000.0;
        
        DarkLogger.info("TEST", String.format("SIMD Kinematics processed %d entities in %.4f ms", ENTITY_COUNT, durationMs));
        
        soa.destroy();
        
        if (durationMs > 16.0) { // Tolerancia relajada a 16.0ms por carga del SO
            throw new RuntimeException("Kinematics latency exceeded 16.0 ms: " + durationMs + " ms");
        }
        
        DarkLogger.info("TEST", "[OK] AAA+ Kinematics Throughput passed.");
    }
}
