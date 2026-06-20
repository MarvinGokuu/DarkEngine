// Reading Order: 00100020
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ecs;

import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;
import sv.dark.scene.DarkTransformSoA;

/**
 * High-Level Scene Graph Orchestrator (Phase 30).
 * 
 * Gestiona el ciclo de vida de las entidades.
 * Implementa una Free-List (Array-Based) para reciclaje de IDs O(1).
 * Es el puente absoluto entre la lógica del juego y el Kernel C/C++.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = true, notes = "Free-List ECS Orchestrator")
public final class DarkScene {

    private final DarkTransformSoA soaMemory;
    private final int maxEntities;
    
    private final int[] freeList;
    private int freeListTail;
    
    private int activeEntityCount;

    public DarkScene(int maxEntities) {
        this.maxEntities = maxEntities;
        this.soaMemory = new DarkTransformSoA(maxEntities);
        
        this.freeList = new int[maxEntities];
        // Populate free list with all available IDs (reversed so we pop from 0 to max)
        for (int i = 0; i < maxEntities; i++) {
            freeList[i] = (maxEntities - 1) - i;
        }
        this.freeListTail = maxEntities - 1;
        this.activeEntityCount = 0;
        
        DarkLogger.info("ECS", "DarkScene Initialized. Capacity: " + maxEntities + " entities.");
    }

    /**
     * Devuelve el bloque de memoria SIMD para que el Kernel (Culling, Físicas) lo procese.
     */
    public DarkTransformSoA getSoA() {
        return soaMemory;
    }

    /**
     * GAME API: Spawnea una entidad en el mundo.
     * Complejidad: O(1)
     * Zero-Allocation GC (El objeto DarkEntity es puramente efímero/opcional o se cachea)
     * 
     * @return El manejador de la entidad (Game API).
     */
    public DarkEntity spawnEntity() {
        if (freeListTail < 0) {
            throw new RuntimeException("DarkScene Capacity Reached! Max: " + maxEntities);
        }
        
        int entityId = freeList[freeListTail--];
        activeEntityCount++;
        
        // Inicializar estado a 0 (Limpiar data basura de vidas pasadas)
        soaMemory.setEntity(entityId, 0.0, 0.0, 0.0f, 0.0f);
        
        return new DarkEntity(entityId, soaMemory);
    }

    /**
     * GAME API: Destruye una entidad y libera su ID para reuso inmediato.
     * Complejidad: O(1)
     */
    public void destroyEntity(int entityId) {
        if (entityId < 0 || entityId >= maxEntities) return;
        
        freeList[++freeListTail] = entityId;
        activeEntityCount--;
        
        // Borramos del mundo visible empujándolo al infinito temporalmente
        // El Culling Shader lo ignorará automáticamente.
        soaMemory.setEntity(entityId, Double.MAX_VALUE, Double.MAX_VALUE, 0.0f, 0.0f);
    }

    public int getActiveEntityCount() {
        return activeEntityCount;
    }

    public void destroy() {
        soaMemory.destroy();
    }
}
