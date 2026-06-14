// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems;

import sv.dark.core.AAACertified;

import java.awt.Graphics2D;
import sv.dark.state.WorldStateFrame;

/**
 * RESPONSIBILITY: Define the base interface for all visual rendering systems.
 * WHY: To strictly separate game logic from presentation, ensuring rendering is read-only.
 * TECHNIQUE: Method injection of the WorldStateFrame and Graphics2D context.
 * GUARANTEES: Zero-allocation render loop and read-only memory access.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
/**
 * RESPONSIBILITY: Core component.
 * WHY: Critical for DarkEngine deterministic execution.
 * TECHNIQUE: Low-latency focused implementation.
 * GUARANTEES: Lock-free execution where applicable.
 */
@AAACertified(
    date = "2026-06-11",
    maxLatencyNs = 0,
    minThroughput = 0,
    alignment = 0,
    lockFree = false,
    offHeap = false,
    notes = "Automatically AAA Certified during Core Audit"
)
public interface DarkRenderSystem {

    /**
     * Renders the world state to the screen.
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * RENDERING CONTRACT
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * PRECONDITIONS (what the Kernel guarantees):
     * 1. g2d != null and is configured for the current frame
     * 2. state != null and is consistent (does not change during render)
     * 3. Called from the AWT/Swing thread
     * 4. State was already updated by all GameSystems
     * 
     * POSTCONDITIONS (what the system must guarantee):
     * 1. Only READS data from the state (does not modify)
     * 2. Draws using g2d (screen projection)
     * 3. Returns fast (< 5ms ideally for 60 FPS)
     * 4. Does not create unnecessary objects (minimize GC)
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * PERFORMANCE
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * TIME BUDGET (60 FPS = 16.6ms per frame):
     * - Game Logic: 10ms
     * - Rendering: 4-5ms (THIS METHOD)
     * - Buffer swap: 1-2ms
     * 
     * OPTIMIZATION TECHNIQUES:
     * 1. Batching: Group similar draw calls
     * 2. Culling: Do not draw what is outside the screen
     * 3. Sprite Atlas: A single texture for multiple sprites
     * 4. Object Pooling: Reuse drawing objects
     * 
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * USAGE EXAMPLE
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * ```java
     * public class SpriteSystem implements DarkRenderSystem {
     * 
     * @Override
     *           public void render(Graphics2D g2d, WorldStateFrame state) {
     *           int entityCount = state.readInt(DarkStateLayout.ENTITY_COUNT);
     * 
     *           for (int i= 0; i< entityCount; i++) {
     *           long base = i* EntityLayout.STRIDE;
     *           double x = state.readDouble(base + EntityLayout.X_OFFSET);
     *           double y = state.readDouble(base + EntityLayout.Y_OFFSET);
     * 
     *           // Draw sprite at (x, y)
     *           g2d.drawImage(sprite, (int)x, (int)y, null);
     *           }
     *           }
     *           }
     *           ```
     * 
     *           = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
     * 
     * @param g2d   Java2D graphic context for drawing.
     *              Configured with transformations, clipping, and compositing.
     * 
     *              COMMON OPERATIONS:
     *              - g2d.drawImage(): Draw textures
     *              - g2d.fillRect(): Draw solid rectangles
     *              - g2d.drawString(): Draw text
     *              - g2d.setColor(): Change drawing color
     * 
     * @param state Immutable snapshot of the world state (Read-Only).
     *              Contains ALL game data in Off-Heap memory.
     * 
     *              ACCESS (READ-ONLY):
     *              - state.readDouble(offset): Read coordinates, etc.
     *              - state.readInt(offset): Read counters, flags, etc.
     * 
     *              FORBIDDEN:
     *              - state.writeXXX(): DO NOT modify the state
     * 
     * @see WorldStateFrame For details on memory access
     * @see EntityLayout For entity offsets
     * @see GameSystem For game logic systems
     */
    void render(Graphics2D g2d, WorldStateFrame state);

    /**
     * Returns the name of the rendering system for debugging.
     * 
     * DEFAULT IMPLEMENTATION: Uses the class name.
     * 
     * @return System name (not null, not empty)
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
// Created: 03/01/2026 23:40
// Role: Software Architect applying ISP + Separation of Concerns
// Principles: ISP, SoC, Observer Pattern (implicit)
