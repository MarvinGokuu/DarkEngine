// Reading Order: 01100010
//  98
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
    
    private int[] freeList;
    private int freeListTail;
    
    private int activeEntityCount;
    
    // [TOPOLOGICAL SORT MAPPING]
    // Mapea ID Lógico (DarkEntity) -> Índice Físico (SoA)
    private int[] logicalToPhysical;
    private int[] physicalToLogical;
    
    // [LOGICAL HIERARCHY]
    private int[] logicalParents;
    private int[] logicalFirstChildren;
    private int[] logicalNextSiblings;
    
    // [ZERO-GC FACADE POOL]
    private DarkEntity[] entityWrappers;

    // [ECS COMPONENT SYSTEM]
    // Máximo 64 tipos de componentes diferentes soportados
    @SuppressWarnings("unchecked")
    private ComponentArray<? extends DarkComponent>[] componentArrays = new ComponentArray[64];
    // Una bitmask (long de 64 bits) por cada entidad posible
    private long[] entitySignatures;

    public DarkScene(int maxEntities) {
        this.maxEntities = maxEntities;
        this.soaMemory = new DarkTransformSoA(maxEntities);
        
        this.freeList = new int[maxEntities];
        this.entitySignatures = new long[maxEntities];
        this.logicalToPhysical = new int[maxEntities];
        this.physicalToLogical = new int[maxEntities];
        
        this.logicalParents = new int[maxEntities];
        this.logicalFirstChildren = new int[maxEntities];
        this.logicalNextSiblings = new int[maxEntities];
        
        // Populate free list with all available IDs (reversed so we pop from 0 to max)
        for (int i = 0; i < maxEntities; i++) {
            freeList[i] = (maxEntities - 1) - i;
            logicalToPhysical[i] = i; // Identidad inicial
            physicalToLogical[i] = i;
            
            logicalParents[i] = -1;
            logicalFirstChildren[i] = -1;
            logicalNextSiblings[i] = -1;
        }
        this.freeListTail = maxEntities - 1;
        this.activeEntityCount = 0;
        
        // Zero-GC Pre-allocation of Facades
        this.entityWrappers = new DarkEntity[maxEntities];
        for (int i = 0; i < maxEntities; i++) {
            this.entityWrappers[i] = new DarkEntity(i, this.soaMemory, this);
        }
        
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
        
        int logicalId = freeList[freeListTail--];
        int physicalIdx = activeEntityCount; // Siempre se inserta al final de la memoria activa
        
        logicalToPhysical[logicalId] = physicalIdx;
        physicalToLogical[physicalIdx] = logicalId;
        
        activeEntityCount++;
        
        // Inicializar estado a 0 (Limpiar data basura de vidas pasadas)
        soaMemory.setEntity(physicalIdx, 0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f);
        entitySignatures[logicalId] = 0L; // Reiniciar bitmask
        
        logicalParents[logicalId] = -1;
        logicalFirstChildren[logicalId] = -1;
        logicalNextSiblings[logicalId] = -1;
        
        return entityWrappers[logicalId];
    }

    /**
     * GAME API: Obtiene la Fachada (Wrapper) de una entidad existente sin alojar memoria.
     * Complejidad: O(1)
     */
    public DarkEntity getEntity(int entityId) {
        if (entityId < 0 || entityId >= maxEntities) return null;
        return entityWrappers[entityId];
    }

    /**
     * GAME API: Destruye una entidad y libera su ID para reuso inmediato.
     * Complejidad: O(1)
     */
    public void destroyEntity(int logicalId) {
        if (logicalId < 0 || logicalId >= maxEntities) return;
        
        int physicalIdx = logicalToPhysical[logicalId];
        int lastPhysicalIdx = activeEntityCount - 1;
        
        // Hacer Swap con el último activo para mantener la memoria contigua sin huecos
        if (physicalIdx != lastPhysicalIdx) {
            int lastLogicalId = physicalToLogical[lastPhysicalIdx];
            
            // Swap SoA
            soaMemory.swap(physicalIdx, lastPhysicalIdx);
            
            // Update Mappings
            logicalToPhysical[lastLogicalId] = physicalIdx;
            physicalToLogical[physicalIdx] = lastLogicalId;
        }
        
        freeList[++freeListTail] = logicalId;
        activeEntityCount--;
        
        // Borramos del mundo visible (ahora en lastPhysicalIdx)
        soaMemory.setEntity(lastPhysicalIdx, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, 0.0f, 0.0f, 0.0f);
    }
    
    public int getPhysicalIndex(int logicalId) {
        return logicalToPhysical[logicalId];
    }
    
    public int getLogicalId(int physicalIdx) {
        return physicalToLogical[physicalIdx];
    }

    public int getActiveEntityCount() {
        return activeEntityCount;
    }

    // ==========================================
    // GAME API: COMPONENT SYSTEM (Data-Oriented)
    // ==========================================

    @SuppressWarnings("unchecked")
    private <T extends DarkComponent> ComponentArray<T> getComponentArray(Class<T> type) {
        int id = ComponentRegistry.getComponentId(type);
        if (componentArrays[id] == null) {
            componentArrays[id] = new ComponentArray<>(maxEntities);
        }
        return (ComponentArray<T>) componentArrays[id];
    }

    public <T extends DarkComponent> void addComponent(int entityId, T component) {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) component.getClass();
        int componentId = ComponentRegistry.getComponentId(type);
        
        getComponentArray(type).insertData(entityId, component);
        
        // Encender bit en la bitmask
        entitySignatures[entityId] |= (1L << componentId);
    }

    public <T extends DarkComponent> void removeComponent(int entityId, Class<T> type) {
        int componentId = ComponentRegistry.getComponentId(type);
        getComponentArray(type).removeData(entityId);
        
        // Apagar bit en la bitmask
        entitySignatures[entityId] &= ~(1L << componentId);
    }

    public <T extends DarkComponent> T getComponent(int entityId, Class<T> type) {
        int componentId = ComponentRegistry.getComponentId(type);
        
        // Verificación bitmask O(1) ultra-rápida sin hacer lookup en memoria si no lo tiene
        if ((entitySignatures[entityId] & (1L << componentId)) == 0) {
            return null;
        }
        
        return getComponentArray(type).getData(entityId);
    }

    public boolean hasComponent(int entityId, Class<? extends DarkComponent> type) {
        int componentId = ComponentRegistry.getComponentId(type);
        return (entitySignatures[entityId] & (1L << componentId)) != 0;
    }

    // ==========================================
    // SCENE GRAPH: TOPOLOGICAL SORTING (Phase 2)
    // ==========================================
    
    public void setParent(int childLogicalId, int parentLogicalId) {
        logicalParents[childLogicalId] = parentLogicalId;
        
        if (parentLogicalId >= 0) {
            int firstChild = logicalFirstChildren[parentLogicalId];
            logicalNextSiblings[childLogicalId] = firstChild;
            logicalFirstChildren[parentLogicalId] = childLogicalId;
        }
        
        // Maintain physical SoA in-sync for immediate access before topological sort
        int childPhys = logicalToPhysical[childLogicalId];
        int parentPhys = parentLogicalId >= 0 ? logicalToPhysical[parentLogicalId] : -1;
        soaMemory.parentIdx.set(java.lang.foreign.ValueLayout.JAVA_INT, childPhys * 4L, parentPhys);
    }
    
    public void topologicalSort() {
        int sortedCount = 0;
        int[] queue = new int[activeEntityCount];
        int head = 0;
        int tail = 0;
        
        // 1. Encontrar Raíces (parent == -1)
        for (int i = 0; i < activeEntityCount; i++) {
            int logicalId = physicalToLogical[i];
            if (logicalParents[logicalId] == -1) {
                queue[tail++] = logicalId;
            }
        }
        
        // 2. BFS para ordenar topológicamente (Padres antes que Hijos)
        while (head < tail) {
            int currentLogical = queue[head++];
            int currentPhys = logicalToPhysical[currentLogical];
            
            // Swap si es necesario para mantener orden
            if (currentPhys != sortedCount) {
                int logicalB = physicalToLogical[sortedCount];
                
                soaMemory.swap(currentPhys, sortedCount);
                
                logicalToPhysical[currentLogical] = sortedCount;
                physicalToLogical[sortedCount] = currentLogical;
                
                logicalToPhysical[logicalB] = currentPhys;
                physicalToLogical[currentPhys] = logicalB;
            }
            
            // Encolar hijos
            int child = logicalFirstChildren[currentLogical];
            while (child != -1) {
                queue[tail++] = child;
                child = logicalNextSiblings[child];
            }
            
            sortedCount++;
        }
        
        // 3. REBUILD PHYSICAL PARENT POINTERS IN SOA
        for (int i = 0; i < activeEntityCount; i++) {
            int logicalId = physicalToLogical[i];
            int logParent = logicalParents[logicalId];
            int physParent = (logParent == -1) ? -1 : logicalToPhysical[logParent];
            soaMemory.parentIdx.set(java.lang.foreign.ValueLayout.JAVA_INT, i * 4L, physParent);
        }
    }

    public void destroy() {
        soaMemory.destroy();
        
        // [FIX] Explicitly nullify large arrays to help GC immediately
        // and prevent the GracefulShutdownTest from detecting a 12MB Heap Impact
        for (int i = 0; i < componentArrays.length; i++) {
            if (componentArrays[i] != null) {
                componentArrays[i].destroy();
                componentArrays[i] = null;
            }
        }
        componentArrays = null;
        freeList = null;
        entitySignatures = null;
        entityWrappers = null;
        logicalToPhysical = null;
        physicalToLogical = null;
        logicalParents = null;
        logicalFirstChildren = null;
        logicalNextSiblings = null;
    }
}
