// Reading Order: 00100024
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ecs;

import sv.dark.core.AAACertified;

/**
 * Array plano, contiguo y fuertemente tipado para el almacenamiento de
 * un Componente Específico.
 * 
 * Garantiza acceso O(1) mapeando el entityId directamente al índice del array.
 * 
 * @param <T> El tipo de DarkComponent almacenado.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = false, notes = "Flat Component Data Storage")
public final class ComponentArray<T extends DarkComponent> {

    private Object[] array;
    private final int maxEntities;

    public ComponentArray(int maxEntities) {
        this.maxEntities = maxEntities;
        this.array = new Object[maxEntities];
    }

    /**
     * Inserta el componente en el índice correspondiente al entityId. O(1).
     */
    public void insertData(int entityId, T component) {
        if (entityId < 0 || entityId >= maxEntities) return;
        array[entityId] = component;
    }

    /**
     * Elimina la referencia al componente. O(1).
     */
    public void removeData(int entityId) {
        if (entityId < 0 || entityId >= maxEntities) return;
        array[entityId] = null;
    }

    /**
     * Devuelve el componente asignado a la entidad, o nulo si no tiene. O(1).
     */
    @SuppressWarnings("unchecked")
    public T getData(int entityId) {
        if (entityId < 0 || entityId >= maxEntities) return null;
        return (T) array[entityId];
    }

    public void destroy() {
        array = null;
    }
}
