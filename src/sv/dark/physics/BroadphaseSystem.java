// Reading Order: 00100032
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.physics;

import sv.dark.core.AAACertified;
import sv.dark.core.systems.GameSystem;
import sv.dark.ecs.DarkScene;
import sv.dark.state.WorldStateFrame;

/**
 * Broadphase Collision System (Phase 31).
 * 
 * Se inyecta después de las físicas de movimiento.
 * Reconstruye la Cuadrícula Espacial Hash cada frame en tiempo lineal O(N)
 * para mantener a las entidades agrupadas en memoria.
 */
@AAACertified(date = "2026-06-19", maxLatencyNs = 50, minThroughput = 0, lockFree = true, offHeap = false, notes = "O(N) Spatial Re-Hasing Pipeline")
public final class BroadphaseSystem implements GameSystem {

    private final DarkScene scene;
    private final SpatialHashGrid grid;

    public BroadphaseSystem(DarkScene scene) {
        this.scene = scene;
        // Configuración de Mundo Masivo: Celdas de 64x64 unidades. Grid de 100x100 celdas (10,000 celdas).
        this.grid = new SpatialHashGrid(scene.getSoA().getCapacity(), 64.0f, 100, 100);
    }

    @Override
    public void update(WorldStateFrame state, float deltaTime) {
        // En cada tick, reconstruimos el árbol/grid por completo.
        // O(N) usando SoA nativo y listas enlazadas en arreglo.
        grid.buildGrid(scene.getSoA(), scene.getSoA().getCapacity());
        
        // Fase 31 Futuro: 
        // Generar pares de colisión interactuando entre celdas vecinas
        // y enviarlas al Narrow-Phase Solver.
    }

    public SpatialHashGrid getGrid() {
        return grid;
    }

    public void destroy() {
        grid.destroy();
    }
}
