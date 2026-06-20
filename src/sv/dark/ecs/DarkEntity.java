// Reading Order: 00100019
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ecs;

import sv.dark.core.AAACertified;
import sv.dark.scene.DarkTransformSoA;
import java.lang.foreign.ValueLayout;

/**
 * Game API: Hybrid ECS Abstraction (Phase 30).
 * 
 * Es la cara orientada a objetos (OOP) que usa el desarrollador de juegos.
 * No contiene variables de memoria (Cero Heap). Todo se traduce en tiempo real
 * a lecturas/escrituras en el bloque contiguo de memoria (SoA) a través de FFI.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "OOP Abstraction over pure SoA Data")
public final class DarkEntity {

    private final int id;
    private final DarkTransformSoA soaMemory;

    /**
     * Instantiated exclusively by DarkScene.spawnEntity().
     */
    DarkEntity(int id, DarkTransformSoA soa) {
        this.id = id;
        this.soaMemory = soa;
    }

    public int getId() {
        return id;
    }

    // ==========================================
    // GAME API: TRANSFORM (64-bit Logic)
    // ==========================================

    public void setPosition(double x, double y) {
        long offset64 = id * 8L;
        soaMemory.globalPosX.set(ValueLayout.JAVA_DOUBLE, offset64, x);
        soaMemory.globalPosY.set(ValueLayout.JAVA_DOUBLE, offset64, y);
        
        // Sincronizar inmediatamente a VRAM view (32-bit)
        long offset32 = id * 4L;
        soaMemory.posX.set(ValueLayout.JAVA_FLOAT, offset32, (float) x);
        soaMemory.posY.set(ValueLayout.JAVA_FLOAT, offset32, (float) y);
    }

    public double getPositionX() {
        return soaMemory.globalPosX.get(ValueLayout.JAVA_DOUBLE, id * 8L);
    }

    public double getPositionY() {
        return soaMemory.globalPosY.get(ValueLayout.JAVA_DOUBLE, id * 8L);
    }

    public void setVelocity(float vx, float vy) {
        long offset = id * 4L;
        soaMemory.velX.set(ValueLayout.JAVA_FLOAT, offset, vx);
        soaMemory.velY.set(ValueLayout.JAVA_FLOAT, offset, vy);
    }

    public float getVelocityX() {
        return soaMemory.velX.get(ValueLayout.JAVA_FLOAT, id * 4L);
    }

    public float getVelocityY() {
        return soaMemory.velY.get(ValueLayout.JAVA_FLOAT, id * 4L);
    }
}
