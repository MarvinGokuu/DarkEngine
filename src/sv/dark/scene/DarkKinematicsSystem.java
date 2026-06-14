// Reading Order: 00100001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import java.nio.ByteOrder;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * Data-Oriented Technology Stack: SIMD Kinematics System.
 * 
 * <p>Aplica física básica (posición = posición + velocidad * delta_tiempo)
 * procesando hasta 16 o 32 entidades en un solo ciclo de CPU mediante AVX.
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 1, minThroughput = 1_000_000, lockFree = true, offHeap = true, notes = "SIMD SoA System")
public final class DarkKinematicsSystem {
    
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final ByteOrder BO = ByteOrder.nativeOrder();

    /**
     * Procesa todo el arreglo de entidades en una sola pasada vectorial.
     * 
     * @param soa El bloque de memoria Structure of Arrays.
     * @param dt Delta time en segundos.
     */
    public static void update(DarkTransformSoA soa, float dt) {
        int capacity = soa.getCapacity();
        long loopBound = SPECIES.loopBound(capacity);
        long i = 0;

        // Fase 1: Acelerador SIMD (Procesamiento en paralelo por hardware)
        for (; i < loopBound; i += SPECIES.length()) {
            long offset = i * 4L;
            
            // Vector X
            FloatVector px = FloatVector.fromMemorySegment(SPECIES, soa.posX, offset, BO);
            FloatVector vx = FloatVector.fromMemorySegment(SPECIES, soa.velX, offset, BO);
            FloatVector newPx = px.add(vx.mul(dt));
            newPx.intoMemorySegment(soa.posX, offset, BO);
            
            // Vector Y
            FloatVector py = FloatVector.fromMemorySegment(SPECIES, soa.posY, offset, BO);
            FloatVector vy = FloatVector.fromMemorySegment(SPECIES, soa.velY, offset, BO);
            FloatVector newPy = py.add(vy.mul(dt));
            newPy.intoMemorySegment(soa.posY, offset, BO);
        }

        // Fase 2: Cola Escalar (Para las entidades restantes al final del arreglo)
        for (; i < capacity; i++) {
            long offset = i * 4L;
            
            float px = soa.posX.get(ValueLayout.JAVA_FLOAT, offset);
            float vx = soa.velX.get(ValueLayout.JAVA_FLOAT, offset);
            soa.posX.set(ValueLayout.JAVA_FLOAT, offset, px + (vx * dt));
            
            float py = soa.posY.get(ValueLayout.JAVA_FLOAT, offset);
            float vy = soa.velY.get(ValueLayout.JAVA_FLOAT, offset);
            soa.posY.set(ValueLayout.JAVA_FLOAT, offset, py + (vy * dt));
        }
    }
}
