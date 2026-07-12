// Reading Order: 01001001
//  73
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import sv.dark.core.AAACertified;

/**
 * Contrato base para un nodo dentro del DarkFrameGraph.
 * Cada pase encapsula una fase específica del pipeline gráfico (ej. Sombras, G-Buffer, PostProcess).
 */
@AAACertified(date = "2026-06-28", maxLatencyNs = 0, minThroughput = 0, lockFree = true, offHeap = true, notes = "Base interface for Render Graph Nodes")
public interface DarkRenderPass {
    
    /**
     * Nombre identificador del pase (para logs y depuración).
     */
    String getName();

    /**
     * Determina si este pase debe ejecutarse en el frame actual.
     * Permite Culling dinámico de pases enteros.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Ejecuta la lógica del pase. 
     * En el futuro, el orquestador inyectará Pipeline Barriers y Layout Transitions antes de llamar a execute().
     * 
     * @param viewMatrix Matriz de Vista de la cámara principal.
     * @param projMatrix Matriz de Proyección de la cámara principal.
     */
    void execute(float[] viewMatrix, float[] projMatrix);

}
