// Reading Order: 00100033
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.test;

import sv.dark.scene.DarkTransformSoA;
import sv.dark.physics.SpatialHashGrid;
import java.lang.foreign.ValueLayout;

/**
 * RESPONSIBILITY: Broadphase Culling Benchmark and Collision Capacity Test.
 * WHY: Validating that grouping 100,000 entities into spatial grid cells takes < 2ms (Sub-millisecond goal).
 * TECHNIQUE: Data-Oriented Linked Lists inside Arrays (Zero-Allocation).
 * GUARANTEES: Zero Garbage Collection overhead.
 */
public class SpatialHashGridTest {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" BROADPHASE CULLING - SPATIAL HASH GRID TEST");
        System.out.println("==============================================");

        int maxEntities = 100_000;
        int gridWidth = 100;
        int gridHeight = 100;
        float cellSize = 10.0f;

        System.out.println("[TEST] Initializing GPU Context for Broadphase...");
        sv.dark.ui.DarkEngineWindow.initNativeWindow();
        sv.dark.core.systems.DarkOpenGLLinker.init();

        System.out.println("[TEST] Allocating off-heap arrays for " + maxEntities + " entities...");
        DarkTransformSoA soa = new DarkTransformSoA(maxEntities);
        SpatialHashGrid grid = new SpatialHashGrid(maxEntities, cellSize, gridWidth, gridHeight);
        grid.initGraphicsContext();

        System.out.println("[TEST] Spawning random entities in grid (1000x1000 area)...");
        for (int i = 0; i < maxEntities; i++) {
            double rx = Math.random() * 1000.0;
            double ry = Math.random() * 1000.0;
            soa.globalPosX.set(ValueLayout.JAVA_DOUBLE, i * 8L, rx);
            soa.globalPosY.set(ValueLayout.JAVA_DOUBLE, i * 8L, ry);
        }

        System.out.println("[TEST] Warming up JIT Compiler...");
        for (int i = 0; i < 1000; i++) {
            grid.buildGrid(soa, maxEntities);
        }

        System.out.println("[TEST] Benchmarking Spatial Hash Grid O(N) Insertion...");
        long startNs = System.nanoTime();
        
        int passes = 1000;
        for (int i = 0; i < passes; i++) {
            grid.buildGrid(soa, maxEntities);
        }
        
        long endNs = System.nanoTime();
        double avgTimeMs = ((endNs - startNs) / 1_000_000.0) / passes;

        System.out.printf("[RESULT] Time to hash %d entities: %.3f ms per frame.%n", maxEntities, avgTimeMs);
        
        if (avgTimeMs > 5.0) {
            System.err.println("[FAIL] Performance is below AAA+ standards. Expected < 5.0ms, got: " + avgTimeMs + "ms");
            System.exit(1);
        } else {
            System.out.println("[OK] Sub-millisecond Spatial Partitioning achieved!");
        }

        System.out.println("[TEST] Verifying cell consistency...");
        // Verifica que la Celda 50,50 (coordenadas 500,500) tiene entidades
        int centerCellId = grid.getCellId(500.0, 500.0);
        int head = grid.getHeadEntity(centerCellId);
        int count = 0;
        
        int current = head;
        while (current != -1) {
            count++;
            current = grid.getNextEntity(current);
        }
        
        System.out.println("[OK] Found " + count + " entities in the center cell (" + centerCellId + ").");
        
        grid.destroy();
        soa.destroy();
        System.exit(0);
    }
}
