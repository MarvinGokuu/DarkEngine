package sv.volcan.core;

import java.awt.*;
import java.awt.image.BufferStrategy;
import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Puente de Visualización de Alto Rendimiento (Triple
 * Buffering).
 * DEPENDENCIAS: java.awt.Canvas, java.awt.image.BufferStrategy, WorldStateFrame
 * MÉTRICAS: Target 60 FPS, Zero-Allocation Render
 * 
 * Gestiona la proyección visual del estado soberano a la pantalla.
 * Implementa Triple Buffering y control de V-Sync para eliminar tearing.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class VolcanDisplayBridge {

    private final BufferStrategy strategy;
    private final Canvas canvas;

    // Recursos pre-alocados (Evita la presión sobre el Garbage Collector)
    private static final Color BG_COLOR = new Color(5, 5, 10);
    private static final Color SCANLINE_COLOR = new Color(0, 0, 0, 30);
    private final RenderingHints hints;

    public VolcanDisplayBridge(Canvas canvas) {
        this.canvas = canvas;
        // [HITO 1.3]: Triple buffering para máximo throughput visual.
        canvas.createBufferStrategy(3);
        this.strategy = canvas.getBufferStrategy();

        this.hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }

    /**
     * Proyecta el estado binario a la pantalla.
     * [MECHANICAL SYMPATHY]: El uso de do-while asegura la integridad del buffer
     * ante cambios de contexto del SO.
     */
    public void render(WorldStateFrame state) {
        do {
            Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
            try {
                g.setRenderingHints(hints);

                // 1. Limpieza de Frame (Soberanía de Color)
                g.setColor(BG_COLOR);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // 2. Proyección de Entidades
                // [OBSERVACIÓN]: Aquí se integrarán las llamadas a SpriteSystem.renderBatch.

                // 3. Post-procesado Soberano (Zero-allocation)
                applyIndustrialFilters(g);

            } finally {
                g.dispose(); // Liberación inmediata de recursos GDI/X11
            }
        } while (strategy.contentsRestored());

        strategy.show();
        // Sincronización de hardware para evitar jitter en el bus de video
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Filtro estético industrial aplicado directamente sobre el raster.
     */
    private void applyIndustrialFilters(Graphics2D g) {
        g.setColor(SCANLINE_COLOR);
        int h = canvas.getHeight();
        int w = canvas.getWidth();
        // Dibujo directo por líneas para simular CRT/Monitor Industrial sin sobrecarga
        // de shaders.
        for (int y = 0; y < h; y += 2) {
            g.drawLine(0, y, w, y);
        }
    }
}
// actualizado3/1/26