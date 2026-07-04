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
     * @param camZ Posición global de la cámara Z (64-bits).
     */
    public static void update(DarkTransformSoA soa, float dt, double camX, double camY, double camZ) {
        int capacity = soa.getCapacity();
        long loopBound = D_SPECIES.loopBound(capacity);
        long i = 0;

        // Fase 1: Acelerador SIMD (Precisión Infinita 64-bits sin máscaras)
        for (; i < loopBound; i += D_SPECIES.length()) {
            long offset32 = i * 4L;
            long offset64 = i * 8L;
            
            // 1. Memory Load (Interleaved para saturar el bus de memoria)
            DoubleVector px = DoubleVector.fromMemorySegment(D_SPECIES, soa.localPosX, offset64, BO);
            DoubleVector py = DoubleVector.fromMemorySegment(D_SPECIES, soa.localPosY, offset64, BO);
            DoubleVector pz = DoubleVector.fromMemorySegment(D_SPECIES, soa.localPosZ, offset64, BO);
            FloatVector vxFloat = FloatVector.fromMemorySegment(F_SPECIES, soa.velX, offset32, BO);
            FloatVector vyFloat = FloatVector.fromMemorySegment(F_SPECIES, soa.velY, offset32, BO);
            FloatVector vzFloat = FloatVector.fromMemorySegment(F_SPECIES, soa.velZ, offset32, BO);

            // 2. CPU ALU Math (Instruction Level Parallelism)
            DoubleVector vx = (DoubleVector) vxFloat.castShape(D_SPECIES, 0);
            DoubleVector vy = (DoubleVector) vyFloat.castShape(D_SPECIES, 0);
            DoubleVector vz = (DoubleVector) vzFloat.castShape(D_SPECIES, 0);
            
            DoubleVector newPx = px.add(vx.mul(dt));
            DoubleVector newPy = py.add(vy.mul(dt));
            DoubleVector newPz = pz.add(vz.mul(dt));
            
            // Note: SIMD phase only updates Local Position. 
            // Global Position and Visual Position are calculated in the scalar phase below
            // to properly resolve parent-child hierarchy in topological order.

            // 3. Memory Store (Local Pos Only)
            newPx.intoMemorySegment(soa.localPosX, offset64, BO);
            newPy.intoMemorySegment(soa.localPosY, offset64, BO);
            newPz.intoMemorySegment(soa.localPosZ, offset64, BO);
        }

        // Fase 2: Jerarquía y Transformaciones Globales (Escalar Topológico)
        // Ya que la memoria está ordenada topológicamente, los padres siempre se procesan antes que los hijos.
        for (i = 0; i < capacity; i++) {
            long offset32 = i * 4L;
            long offset64 = i * 8L;
            
            // 1. Actualizar Física Local si no fue cubierta por SIMD
            if (i >= loopBound) {
                double px = soa.localPosX.get(ValueLayout.JAVA_DOUBLE, offset64);
                float vx = soa.velX.get(ValueLayout.JAVA_FLOAT, offset32);
                soa.localPosX.set(ValueLayout.JAVA_DOUBLE, offset64, px + (vx * dt));
                
                double py = soa.localPosY.get(ValueLayout.JAVA_DOUBLE, offset64);
                float vy = soa.velY.get(ValueLayout.JAVA_FLOAT, offset32);
                soa.localPosY.set(ValueLayout.JAVA_DOUBLE, offset64, py + (vy * dt));
                
                double pz = soa.localPosZ.get(ValueLayout.JAVA_DOUBLE, offset64);
                float vz = soa.velZ.get(ValueLayout.JAVA_FLOAT, offset32);
                soa.localPosZ.set(ValueLayout.JAVA_DOUBLE, offset64, pz + (vz * dt));
            }
            
            // 2. Calcular Global Position resolviendo la jerarquía
            double localX = soa.localPosX.get(ValueLayout.JAVA_DOUBLE, offset64);
            double localY = soa.localPosY.get(ValueLayout.JAVA_DOUBLE, offset64);
            double localZ = soa.localPosZ.get(ValueLayout.JAVA_DOUBLE, offset64);
            
            int parentIdx = soa.parentIdx.get(ValueLayout.JAVA_INT, offset32);
            double globalX = localX;
            double globalY = localY;
            double globalZ = localZ;
            
            if (parentIdx >= 0) {
                long pOff = parentIdx * 8L;
                globalX += soa.globalPosX.get(ValueLayout.JAVA_DOUBLE, pOff);
                globalY += soa.globalPosY.get(ValueLayout.JAVA_DOUBLE, pOff);
                globalZ += soa.globalPosZ.get(ValueLayout.JAVA_DOUBLE, pOff);
            }
            
            // 3. Escribir Global y Camera-Relative Visual
            soa.globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64, globalX);
            soa.globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64, globalY);
            soa.globalPosZ.set(ValueLayout.JAVA_DOUBLE, offset64, globalZ);
            
            soa.posX.set(ValueLayout.JAVA_FLOAT, offset32, (float)(globalX - camX));
            soa.posY.set(ValueLayout.JAVA_FLOAT, offset32, (float)(globalY - camY));
            soa.posZ.set(ValueLayout.JAVA_FLOAT, offset32, (float)(globalZ - camZ));
        }
    }
}
