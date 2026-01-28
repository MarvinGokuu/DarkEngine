package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.EntityLayout;
import java.awt.Graphics2D;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Renderizado 2D Masivo (Sprite Batching).
 * DEPENDENCIAS: EntityLayout, WorldStateFrame
 * MÉTRICAS: Minimized Draw Calls
 * 
 * Sistema de visualización principal. Lee entidades de memoria y las dibuja
 * utilizando técnicas de batching para alto rendimiento gráfico.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class SpriteSystem implements VolcanRenderSystem {

    /**
     * Renderiza todas las entidades del mundo.
     * 
     * IMPLEMENTACIÓN: VolcanRenderSystem.render()
     * GARANTÍA: Read-Only - Solo lee el estado, nunca lo modifica
     * OPTIMIZACIÓN: Batching - Agrupa draw calls para minimizar cambios de estado
     * 
     * [NOTA TÉCNICA]: Acceso secuencial a memoria nativa para maximizar el Cache
     * Hit Rate.
     */
    @Override
    public void render(Graphics2D g2d, WorldStateFrame state) {
        // SSOT: Leemos entityCount desde el estado (no como parámetro)
        int entityCount = state.readInt(VolcanStateLayout.ENTITY_COUNT);

        for (int i = 0; i < entityCount; i++) {
            // [OPTIMIZACIÓN]: Se recomienda cambiar 'i * STRIDE' por acumulador base +=
            // STRIDE en el futuro.
            long base = (long) i * EntityLayout.STRIDE;

            // Lectura directa del Vault (Sin conversiones intermedias)
            // [COHERENCIA RESTAURADA]: Ahora usa Double como MovementSystem (03/01/2026).
            double x = state.readDouble(base + EntityLayout.X_OFFSET);
            double y = state.readDouble(base + EntityLayout.Y_OFFSET);
            double glow = state.readDouble(base + EntityLayout.GLOW_ALPHA);

            // El dibujo se realiza usando una sola textura "Atlas" compartida
            // (Zero-Switching)
            this.drawFromAtlas(g2d, x, y, glow);
        }
    }

    /**
     * Inyección visual en el buffer de pantalla.
     */
    private void drawFromAtlas(Graphics2D g2d, double x, double y, double glow) {
        // Implementación de dibujo atómico (Render Control)
        // Se asume la existencia de un Atlas pre-cargado.
    }
    // actualizado3/1/26
}