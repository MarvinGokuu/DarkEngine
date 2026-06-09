package sv.dark.core.systems;

import sv.dark.state.WorldStateFrame;

/**
 * AUTORIDAD: Dark
 * RESPONSABILIDAD: Sistema de renderizado lógico.
 * METRICAS: Local metrics aggregation to prevent false sharing.
 */
public final class RenderSystem implements GameSystem {

    private int processedCount = 0;

    public int getProcessedCount() {
        return processedCount;
    }

    public void incrementProcessedCount() {
        processedCount++;
    }

    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        processedCount++; // Incrementar métrica local en cada update
    }
}
