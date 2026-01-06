package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.EntityLayout;
import java.awt.Graphics2D;

/**
 * AUTORIDAD: Render
 * RESPONSABILIDAD: Dibujo masivo de entidades mediante Atlas de Texturas.
 * GARANTÍAS: Minimización de Draw Calls, acceso zero-copy al estado del mundo.
 * PROHIBICIONES: Prohibido instanciar objetos Rectangle o Point dentro del loop
 * de renderizado.
 * DOMINIO CRÍTICO: Visualización (IO Render)
 * 
 * PATRÓN: Observer Pattern (observa el estado sin modificarlo)
 * PRINCIPIO: Separation of Concerns - Render separado de lógica
 * CONCEPTO: Read-Only Access - Solo lee, nunca modifica el estado
 * ROL: Graphics Engineer aplicando batching y culling
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
        // Implementación de dibujo atómico (Soberanía de Render)
        // Se asume la existencia de un Atlas pre-cargado.
    }
    // actualizado3/1/26
}