package sv.volcan.core.systems;

import java.awt.Graphics2D;
import sv.volcan.state.WorldStateFrame;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * CONTRATO DE SISTEMA DE RENDERIZADO (Render System Contract)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * AUTORIDAD: Volcan (Arquitectura de Visualización)
 * RESPONSABILIDAD: Definir el contrato para sistemas que proyectan el estado
 * del mundo a la pantalla.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * SEPARACIÓN DE RESPONSABILIDADES
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * DIFERENCIA CON SovereignSystem:
 * 
 * SovereignSystem (Lógica de Juego):
 * - Input: WorldStateFrame
 * - Output: WorldStateFrame modificado
 * - Características: Determinista, Zero-Allocation, Thread-Safe
 * - Ejemplos: MovementSystem, PlayerSystem, PhysicsSystem
 * 
 * VolcanRenderSystem (Visualización):
 * - Input: WorldStateFrame + Graphics2D
 * - Output: Píxeles en pantalla
 * - Características: No-determinista, I/O pesado, Single-threaded
 * - Ejemplos: SpriteSystem, ParticleRenderer, UIRenderer
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * CONCEPTOS DE INGENIERÍA APLICADOS
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * 1. PRINCIPIO SOLID: Interface Segregation Principle (ISP)
 * ROL EJECUTOR: Software Architect
 * APLICACIÓN: Separamos render de lógica porque tienen diferentes contratos.
 * BENEFICIO: Los sistemas de lógica no necesitan saber de Graphics2D.
 * 
 * 2. PRINCIPIO: Separation of Concerns (SoC)
 * ROL EJECUTOR: Software Engineer
 * APLICACIÓN: Lógica (transformación) vs Presentación (visualización).
 * BENEFICIO: Puedes cambiar el renderizado sin afectar la lógica.
 * 
 * 3. PATRÓN: Observer Pattern (implícito)
 * ROL EJECUTOR: Software Architect
 * APLICACIÓN: El render "observa" el estado sin modificarlo.
 * BENEFICIO: El estado es la fuente de verdad, el render solo proyecta.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * GARANTÍAS DEL CONTRATO
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * ✅ GARANTÍA 1: Read-Only Access
 * - El render SOLO LEE el estado, nunca lo modifica
 * - Esto permite renderizar en cualquier momento sin afectar la simulación
 * 
 * ✅ GARANTÍA 2: Single-Threaded
 * - El render siempre se ejecuta en el thread de AWT/Swing
 * - No hay concurrencia en el renderizado
 * 
 * ✅ GARANTÍA 3: Frame-Based
 * - Cada render corresponde a un frame del juego
 * - El estado es consistente durante todo el render
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * PROHIBICIONES
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * ❌ PROHIBIDO 1: Modificar el estado
 * RAZÓN: Rompe la separación lógica/presentación
 * EJEMPLO INCORRECTO: state.writeDouble(...) dentro de render()
 * 
 * ❌ PROHIBIDO 2: Lógica de juego en render
 * RAZÓN: El render puede saltearse (vsync, lag), la lógica no
 * EJEMPLO INCORRECTO: Calcular física en render()
 * 
 * ❌ PROHIBIDO 3: Acceso a red o archivos
 * RAZÓN: El render debe ser rápido (< 16ms para 60 FPS)
 * EJEMPLO INCORRECTO: Cargar texturas durante render()
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * DOMINIO CRÍTICO: Visualización / Presentación
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * @author VOLCAN Engine Team
 * @version 3.0
 * @since 2026-01-03
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
