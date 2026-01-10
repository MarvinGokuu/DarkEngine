package sv.volcan.core.systems;

import java.awt.Graphics2D;
import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Contrato Fundamental de Sistema de Renderizado.
 * DEPENDENCIAS: WorldStateFrame, java.awt.Graphics2D
 * MÉTRICAS: Zero-Allocation Render Loop
 * 
 * Interfaz base para sistemas de visualización. Separa estrictamente la lógica
 * (SovereignSystem)
 * de la presentación (VolcanRenderSystem), garantizando que el renderizado sea
 * de solo lectura.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public interface VolcanRenderSystem {

    /**
     * Renderiza el estado del mundo a la pantalla.
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * CONTRATO DE RENDERIZADO
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * PRECONDICIONES (lo que el Kernel garantiza):
     * 1. g2d != null y está configurado para el frame actual
     * 2. state != null y es consistente (no cambia durante render)
     * 3. Se llama desde el thread de AWT/Swing
     * 4. El estado ya fue actualizado por todos los SovereignSystems
     * 
     * POSTCONDICIONES (lo que el sistema debe garantizar):
     * 1. Solo LEE datos del state (no modifica)
     * 2. Dibuja usando g2d (proyección a pantalla)
     * 3. Retorna rápido (< 5ms idealmente para 60 FPS)
     * 4. No crea objetos innecesarios (minimizar GC)
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * PERFORMANCE
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * PRESUPUESTO DE TIEMPO (60 FPS = 16.6ms por frame):
     * - Lógica de juego: 10ms
     * - Renderizado: 4-5ms (ESTE MÉTODO)
     * - Buffer swap: 1-2ms
     * 
     * TÉCNICAS DE OPTIMIZACIÓN:
     * 1. Batching: Agrupar draw calls similares
     * 2. Culling: No dibujar lo que está fuera de pantalla
     * 3. Sprite Atlas: Una sola textura para múltiples sprites
     * 4. Object Pooling: Reusar objetos de dibujo
     * 
     * ═══════════════════════════════════════════════════════════════════════
     * EJEMPLO DE USO
     * ═══════════════════════════════════════════════════════════════════════
     * 
     * ```java
     * public class SpriteSystem implements VolcanRenderSystem {
     * 
     * @Override
     *           public void render(Graphics2D g2d, WorldStateFrame state) {
     *           int entityCount = state.readInt(VolcanStateLayout.ENTITY_COUNT);
     * 
     *           for (int i = 0; i < entityCount; i++) {
     *           long base = i * EntityLayout.STRIDE;
     *           double x = state.readDouble(base + EntityLayout.X_OFFSET);
     *           double y = state.readDouble(base + EntityLayout.Y_OFFSET);
     * 
     *           // Dibujar sprite en (x, y)
     *           g2d.drawImage(sprite, (int)x, (int)y, null);
     *           }
     *           }
     *           }
     *           ```
     * 
     *           ═══════════════════════════════════════════════════════════════════════
     * 
     * @param g2d   Contexto gráfico de Java2D para dibujar.
     *              Configurado con transformaciones, clipping, y compositing.
     * 
     *              OPERACIONES COMUNES:
     *              - g2d.drawImage(): Dibujar texturas
     *              - g2d.fillRect(): Dibujar rectángulos sólidos
     *              - g2d.drawString(): Dibujar texto
     *              - g2d.setColor(): Cambiar color de dibujo
     * 
     * @param state Snapshot inmutable del estado del mundo (Read-Only).
     *              Contiene TODOS los datos del juego en memoria Off-Heap.
     * 
     *              ACCESO (SOLO LECTURA):
     *              - state.readDouble(offset): Leer coordenadas, etc.
     *              - state.readInt(offset): Leer contadores, flags, etc.
     * 
     *              PROHIBIDO:
     *              - state.writeXXX(): NO modificar el estado
     * 
     * @see WorldStateFrame Para detalles de acceso a memoria
     * @see EntityLayout Para offsets de entidades
     * @see SovereignSystem Para sistemas de lógica de juego
     */
    void render(Graphics2D g2d, WorldStateFrame state);

    /**
     * Retorna el nombre del sistema de renderizado para debugging.
     * 
     * IMPLEMENTACIÓN POR DEFECTO: Usa el nombre de la clase.
     * 
     * @return Nombre del sistema (no null, no vacío)
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
// Creado: 03/01/2026 23:40
// Rol: Software Architect aplicando ISP + Separation of Concerns
// Principios: ISP, SoC, Observer Pattern (implícito)
