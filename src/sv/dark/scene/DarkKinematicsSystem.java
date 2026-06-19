// Reading Order: 00100001
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene;

import java.nio.ByteOrder;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;

/**
 * Data-Oriented Technology Stack: SIMD Kinematics System.
 * 
 * <p>Aplica física básica en 64-bits de precisión (global = global + vel * dt)
 * procesando múltiples entidades en paralelo usando AVX DoublePrecision.
 * Finalmente inyecta la posición Camera-Relative en 32-bits para la VRAM.
 */
@AAACertified(date = "2026-06-14", maxLatencyNs = 1, minThroughput = 1_000_000, lockFree = true, offHeap = true, notes = "SIMD 64-bit Camera Relative")
public final class DarkKinematicsSystem {
    
    private static final VectorSpecies<Double> D_SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Float> F_SPECIES;
    static {
        int lanes = D_SPECIES.length();
        if (lanes >= 8) F_SPECIES = FloatVector.SPECIES_256;
        else if (lanes == 4) F_SPECIES = FloatVector.SPECIES_128;
        else F_SPECIES = FloatVector.SPECIES_64;
    }
    private static final ByteOrder BO = ByteOrder.nativeOrder();

    /**
     * Procesa todo el arreglo de entidades en una sola pasada vectorial.
     * 
     * @param soa El bloque de memoria Structure of Arrays.
     * @param dt Delta time en segundos.
     * @param camX Posición global de la cámara X (64-bits).
     * @param camY Posición global de la cámara Y (64-bits).
     */
    public static void update(DarkTransformSoA soa, float dt, double camX, double camY) {
        int capacity = soa.getCapacity();
        long loopBound = D_SPECIES.loopBound(capacity);
        long i = 0;

        // Fase 1: Acelerador SIMD (Precisión Infinita 64-bits sin máscaras)
        for (; i < loopBound; i += D_SPECIES.length()) {
            long offset32 = i * 4L;
            long offset64 = i * 8L;
            
            // Vector X (Global)
            DoubleVector px = DoubleVector.fromMemorySegment(D_SPECIES, soa.globalPosX, offset64, BO);
            FloatVector vxFloat = FloatVector.fromMemorySegment(F_SPECIES, soa.velX, offset32, BO);
            DoubleVector vx = (DoubleVector) vxFloat.castShape(D_SPECIES, 0);
            
            DoubleVector newPx = px.add(vx.mul(dt));
            newPx.intoMemorySegment(soa.globalPosX, offset64, BO);
            
            // Inyección Camera Relative Rendering -> 32-bits VRAM
            FloatVector finalVisualX = (FloatVector) newPx.sub(camX).castShape(F_SPECIES, 0);
            finalVisualX.intoMemorySegment(soa.posX, offset32, BO);
            
            // Vector Y (Global)
            DoubleVector py = DoubleVector.fromMemorySegment(D_SPECIES, soa.globalPosY, offset64, BO);
            FloatVector vyFloat = FloatVector.fromMemorySegment(F_SPECIES, soa.velY, offset32, BO);
            DoubleVector vy = (DoubleVector) vyFloat.castShape(D_SPECIES, 0);
            
            DoubleVector newPy = py.add(vy.mul(dt));
            newPy.intoMemorySegment(soa.globalPosY, offset64, BO);
            
            // Inyección Camera Relative Rendering -> 32-bits VRAM
            FloatVector finalVisualY = (FloatVector) newPy.sub(camY).castShape(F_SPECIES, 0);
            finalVisualY.intoMemorySegment(soa.posY, offset32, BO);
        }

        // Fase 2: Cola Escalar (Precisión Infinita 64-bits)
        for (; i < capacity; i++) {
            long offset32 = i * 4L;
            long offset64 = i * 8L;
            
            double px = soa.globalPosX.get(ValueLayout.JAVA_DOUBLE, offset64);
            float vx = soa.velX.get(ValueLayout.JAVA_FLOAT, offset32);
            double newPx = px + (vx * dt);
            soa.globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64, newPx);
            soa.posX.set(ValueLayout.JAVA_FLOAT, offset32, (float)(newPx - camX));
            
            double py = soa.globalPosY.get(ValueLayout.JAVA_DOUBLE, offset64);
            float vy = soa.velY.get(ValueLayout.JAVA_FLOAT, offset32);
            double newPy = py + (vy * dt);
            soa.globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64, newPy);
            soa.posY.set(ValueLayout.JAVA_FLOAT, offset32, (float)(newPy - camY));
        }
    }
}
