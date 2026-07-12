// Reading Order: 10001100
//  140
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import sv.dark.core.AAACertified;
import sv.dark.core.systems.GameSystem;
import sv.dark.ecs.DarkScene;
import sv.dark.state.WorldStateFrame;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import java.nio.ByteOrder;
import java.lang.foreign.ValueLayout;

/**
 * Narrowphase Collision System (Phase 31).
 * 
 * Itera sobre la Cuadrícula Espacial Hash (Broadphase) y despacha verificaciones
 * de colisiones precisas únicamente para las entidades que comparten vecindario espacial.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1500, minThroughput = 0, lockFree = true, offHeap = false, notes = "O(N log N) Narrowphase Dispatcher")
public final class NarrowphaseSystem implements GameSystem {

    private final DarkScene scene;
    private final SpatialHashGrid grid;
    // Asumiremos que el ColliderSoA lo inyecta el Kernel, pero para simplificar
    // permitiremos que el desarrollador pase un manejador nativo.
    private DarkColliderSoA colliders;

    public NarrowphaseSystem(DarkScene scene, SpatialHashGrid grid, DarkColliderSoA colliders) {
        this.scene = scene;
        this.grid = grid;
        this.colliders = colliders;
    }

    @Override
    public void update(WorldStateFrame state, float deltaTime) {
        int maxEntities = scene.getSoA().getCapacity();
        VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
        int upperBound = SPECIES.loopBound(maxEntities);

        // Vectorized SIMD loop (8 entities per instruction on AVX-512)
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            DoubleVector xVec = DoubleVector.fromMemorySegment(SPECIES, scene.getSoA().globalPosX, i * 8L, ByteOrder.nativeOrder());
            VectorMask<Double> activeMask = xVec.compare(VectorOperators.NE, Double.MAX_VALUE);

            if (activeMask.anyTrue()) {
                long bits = activeMask.toLong();
                for (int j = 0; j < SPECIES.length(); j++) {
                    if ((bits & (1L << j)) != 0) {
                        int entityA = i + j;
                        double posX = xVec.lane(j);
                        double posY = scene.getSoA().globalPosY.get(ValueLayout.JAVA_DOUBLE, entityA * 8L);
                        int cellId = grid.getCellId(posX, posY);
                        checkCell(entityA, cellId);
                    }
                }
            }
        }

        // Tail loop for remaining entities
        for (int i = upperBound; i < maxEntities; i++) {
            double posX = scene.getSoA().globalPosX.get(ValueLayout.JAVA_DOUBLE, i * 8L);
            if (posX == Double.MAX_VALUE) continue;
            double posY = scene.getSoA().globalPosY.get(ValueLayout.JAVA_DOUBLE, i * 8L);
            int cellId = grid.getCellId(posX, posY);
            checkCell(i, cellId);
        }
    }

    private void checkCell(int entityA, int cellId) {
        int entityB = grid.getHeadEntity(cellId);
        
        while (entityB != -1) {
            // Regla de ID para no probar el par A-B y luego B-A
            if (entityA < entityB) {
                // Despachar al solver matemático estático
                DarkCollisionSolver.resolveCircleCircle(entityA, entityB, scene.getSoA(), colliders);
            }
            entityB = grid.getNextEntity(entityB);
        }
    }

    @Override
    public String[] getDependencies() {
        return new String[]{"BroadphaseSystem"};
    }
}
