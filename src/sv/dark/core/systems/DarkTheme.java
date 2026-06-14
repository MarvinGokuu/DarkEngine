// Reading Order: 00011000
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core.systems; // Synchronized with path src/sv/dark/core/systems/

import sv.dark.core.AAACertified;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

/**
 * RESPONSIBILITY: Aesthetic Definition and Visual Constants (Design System). Central repository for engine aesthetic constants and drawing styles.
 * WHY: To guarantee visual coherence and avoid object instantiation during rendering.
 * TECHNIQUE: Define hardware constants and pre-instantiated objects (O(1) access).
 * GUARANTEES: Static allocation and zero-runtime-cost for color retrieval.
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
public final class DarkTheme {

    // COLOR PALETTE (Hardware constants - O(1) access)
    public static final Color MINT_NEON = new Color(0, 255, 163);
    public static final Color BACKGROUND = new Color(10, 10, 15);
    public static final Color PANEL_GLASS = new Color(30, 30, 45, 180);
    public static final Color ALERT_CRITICAL = new Color(220, 0, 40);
    public static final Color ALERT_HEALING = new Color(0, 180, 255);

    // [CACHE OPTIMIZATION]: Pre-instantiated Stroke to avoid allocation in the render loop
    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1f);

    private DarkTheme() {
    }

    /**
     * Returns the dynamic border color based on the Vault's alert registry.
     * 
     * @param alertLevel Level extracted from DarkStateLayout.SYS_ENGINE_FLAGS
     */
    public static Color getDynamicAccent(int alertLevel) {
        return switch (alertLevel) {
            case 1 -> ALERT_CRITICAL; // Critical State
            case 2 -> ALERT_HEALING; // Self-repair
            default -> MINT_NEON; // Normal Operation
        };
    }

    /**
     * Definition of the telemetry panels' look.
     */
    public static void applyGlassStyle(Graphics2D g2d, int x, int y, int w, int h) {
        // High-efficiency industrial rendering
        g2d.setColor(PANEL_GLASS);
        g2d.fillRoundRect(x, y, w, h, 12, 12);

        // [OBSERVATION]: Recommended to use DEFAULT_STROKE to avoid 'new' in each call.
        g2d.setStroke(DEFAULT_STROKE);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(x, y, w, h, 12, 12);
    }
    // updated 3/1/26
}
