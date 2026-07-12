// Reading Order: 01101001
//  105
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import java.lang.foreign.ValueLayout;
import sv.dark.core.AAACertified;
import sv.dark.scene.DarkTransformSoA;

/**
 * Data-Oriented Collision Solver (Narrowphase Fast-Path).
 * 
 * Implementa resolución de colisiones geométricas usando matemática escalar veloz,
 * con preparación para AVX. Cubre la intersección exacta (Narrowphase) y
 * el cálculo de la repulsión y rebote elástico (RigidBody Dynamics).
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Static Math Pipeline for Collisions")
public final class DarkCollisionSolver {

    // Evitar instanciación
    private DarkCollisionSolver() {}

    /**
     * Resuelve colisión elástica entre dos círculos.
     * Retorna true si hubo colisión, false si no.
     */
    public static boolean resolveCircleCircle(int idA, int idB, DarkTransformSoA tSoa, DarkColliderSoA cSoa) {
        long offset64A = idA * 8L;
        long offset64B = idB * 8L;
        long offset32A = idA * 4L;
        long offset32B = idB * 4L;

        // 1. Obtener Posiciones (64-bit)
        double ax = tSoa.globalPosX.get(ValueLayout.JAVA_DOUBLE, offset64A);
        double ay = tSoa.globalPosY.get(ValueLayout.JAVA_DOUBLE, offset64A);
        double bx = tSoa.globalPosX.get(ValueLayout.JAVA_DOUBLE, offset64B);
        double by = tSoa.globalPosY.get(ValueLayout.JAVA_DOUBLE, offset64B);

        // 2. Obtener Radios
        float rA = cSoa.radius.get(ValueLayout.JAVA_FLOAT, offset32A);
        float rB = cSoa.radius.get(ValueLayout.JAVA_FLOAT, offset32B);

        // 3. Chequear Distancia Cuadrada (Fast Euclidean)
        double dx = bx - ax;
        double dy = by - ay;
        double distSq = (dx * dx) + (dy * dy);
        double radiusSum = rA + rB;

        if (distSq >= (radiusSum * radiusSum)) {
            return false; // No hay colisión
        }

        // --- COLISIÓN DETECTADA ---
        
        double dist = Math.sqrt(distSq);
        if (dist == 0.0) {
            // Entidades exactamente en el mismo punto (evitar NaN)
            dx = 1.0;
            dy = 0.0;
            dist = 1.0;
        }

        // Vector Normal (Dirección del rebote)
        double nx = dx / dist;
        double ny = dy / dist;

        // Profundidad de penetración
        double penetration = radiusSum - dist;

        // 4. Separación Posicional (Resolver superposición instantáneamente)
        // Movemos las masas proporcionalmente. (Para simplificar, 50/50 aquí o según masa)
        double sepX = nx * (penetration / 2.0);
        double sepY = ny * (penetration / 2.0);
        
        tSoa.globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64A, ax - sepX);
        tSoa.globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64A, ay - sepY);
        tSoa.globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64B, bx + sepX);
        tSoa.globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64B, by + sepY);

        // 5. Dinámicas de Cuerpo Rígido (Conservación de Momento)
        float massA = cSoa.mass.get(ValueLayout.JAVA_FLOAT, offset32A);
        float massB = cSoa.mass.get(ValueLayout.JAVA_FLOAT, offset32B);
        
        // Si la masa es 0, asumimos masa infinita (Estático)
        boolean aStatic = (massA == 0.0f);
        boolean bStatic = (massB == 0.0f);
        if (aStatic && bStatic) return true; // Ambos son inamovibles

        float invMassA = aStatic ? 0.0f : 1.0f / massA;
        float invMassB = bStatic ? 0.0f : 1.0f / massB;

        float vxA = tSoa.velX.get(ValueLayout.JAVA_FLOAT, offset32A);
        float vyA = tSoa.velY.get(ValueLayout.JAVA_FLOAT, offset32A);
        float vxB = tSoa.velX.get(ValueLayout.JAVA_FLOAT, offset32B);
        float vyB = tSoa.velY.get(ValueLayout.JAVA_FLOAT, offset32B);

        // Velocidad relativa
        double rvx = vxB - vxA;
        double rvy = vyB - vyA;

        // Velocidad a lo largo de la normal
        double velAlongNormal = (rvx * nx) + (rvy * ny);

        // Si se están separando, no aplicar impulso
        if (velAlongNormal > 0) return true;

        // Calcular Restitución (Bounciness) - Tomamos el mínimo de ambas
        float restA = cSoa.restitution.get(ValueLayout.JAVA_FLOAT, offset32A);
        float restB = cSoa.restitution.get(ValueLayout.JAVA_FLOAT, offset32B);
        double e = Math.min(restA, restB);

        // Magnitud del impulso de restitución (J)
        double j = -(1.0 + e) * velAlongNormal;
        j /= (invMassA + invMassB);

        // Aplicar Impulso Vectorial
        double impulseX = j * nx;
        double impulseY = j * ny;

        if (!aStatic) {
            tSoa.velX.set(ValueLayout.JAVA_FLOAT, offset32A, (float)(vxA - (impulseX * invMassA)));
            tSoa.velY.set(ValueLayout.JAVA_FLOAT, offset32A, (float)(vyA - (impulseY * invMassA)));
        }
        
        if (!bStatic) {
            tSoa.velX.set(ValueLayout.JAVA_FLOAT, offset32B, (float)(vxB + (impulseX * invMassB)));
            tSoa.velY.set(ValueLayout.JAVA_FLOAT, offset32B, (float)(vyB + (impulseY * invMassB)));
        }

        return true;
    }
}
