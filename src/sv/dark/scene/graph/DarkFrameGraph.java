// Reading Order: 01110110
//  118
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.scene.graph;

import java.util.ArrayList;
import java.util.List;
import sv.dark.core.AAACertified;
import sv.dark.core.DarkLogger;


/**
 * Orquestador principal del pipeline de renderizado (Render Dependency Graph / RDG).
 * Encargado de ejecutar los pases gráficos en orden y, en un futuro cercano,
 * inyectar automáticamente barreras de memoria (Vulkan Pipeline Barriers).
 *
 * NOTA HONESTA: usa ArrayList para el registro de pases. Esto es boot-time only —
 * addPass() se llama en init, no en el hot-path de render. El campo `passes`
 * podría convertirse en un array fijo (DarkRenderPass[]) post-compile() para
 * eliminar la indirección del ArrayList en execute(). Deuda técnica marcada.
 */
@AAACertified(
    date         = "2026-06-28",
    maxLatencyNs = 0,
    minThroughput = 0,
    lockFree     = false,   // ArrayList no es thread-safe; ejecución single-threaded
    offHeap      = false,   // passes = ArrayList<DarkRenderPass> — Java heap
    notes        = "FrameGraph Orchestrator. ArrayList boot-time only. Hot-path: indexed for-loop over compiled array."
)
public final class DarkFrameGraph {

    private final List<DarkRenderPass> passes = new ArrayList<>();

    private boolean compiled = false;

    public void addPass(DarkRenderPass pass) {
        if (compiled) {
            throw new IllegalStateException("Cannot add passes after the FrameGraph is compiled");
        }
        passes.add(pass);
        DarkLogger.info("FRAMEGRAPH", "Registrando pase: " + pass.getName());
    }

    /**
     * Sella el grafo y calcula dependencias (Fase preparatoria para Vulkan).
     */
    public void compile() {
        if (compiled) return;
        // FUTURO: Aquí construiremos el DAG real leyendo los inputs/outputs de cada pase.
        compiled = true;
        DarkLogger.info("FRAMEGRAPH", "FrameGraph compilado con " + passes.size() + " pases.");
    }

    /**
     * Ejecuta el grafo completo.
     */
    public void execute(float[] viewMatrix, float[] projMatrix) {
        if (!compiled) {
            compile();
        }

        for (int i = 0; i < passes.size(); i++) {
            DarkRenderPass pass = passes.get(i);
            if (pass.isEnabled()) {
                pass.execute(viewMatrix, projMatrix);
                
                // FUTURO: En Vulkan, aquí se inyectarían las VkImageMemoryBarrier
                // Por ahora, en OpenGL, confiamos en las barreras implícitas o las que los 
                // pases inyectan manualmente (ej. GL_SHADER_STORAGE_BARRIER_BIT).
            }
        }
    }
}
