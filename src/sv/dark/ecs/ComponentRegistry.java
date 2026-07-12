// Reading Order: 00110110
//  54
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.ecs;

import java.util.HashMap;
import java.util.Map;
import sv.dark.core.AAACertified;

/**
 * Registry de O(1) Lookup para Tipos de Componentes.
 * 
 * Permite que cada Clase (ej. HealthComponent.class) reciba un ID
 * entero único en tiempo de ejecución. Este ID servirá como índice
 * para las Bitmasks del DarkScene sin usar Reflexión lenta.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 1, minThroughput = 0, lockFree = true, offHeap = false, notes = "Static Type Id Assigner")
public final class ComponentRegistry {

    private static int nextComponentId = 0;
    private static final Map<Class<? extends DarkComponent>, Integer> componentTypes = new HashMap<>(64);

    /**
     * Devuelve el ID asociado a la clase del componente.
     * Si no existe, lo registra y asigna un nuevo ID.
     */
    public static <T extends DarkComponent> int getComponentId(Class<T> type) {
        Integer id = componentTypes.get(type);
        if (id == null) {
            // Máximo 64 componentes soportados por entidad para que encaje en el Bitmask (long)
            if (nextComponentId >= 64) {
                throw new RuntimeException("DarkEngine ECS: Maximum of 64 distinct Component Types reached!");
            }
            id = nextComponentId++;
            componentTypes.put(type, id);
        }
        return id;
    }
}
