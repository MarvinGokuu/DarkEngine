// Reading Order: 00010100
// SPDX-FileCopyrightText: 2026 Marvin Alexander Flores Canales
// SPDX-License-Identifier: LGPL-3.0-or-later
package sv.dark.core;

import java.awt.*;
import sv.dark.state.DarkStateVault;
import sv.dark.state.DarkStateLayout;
import sv.dark.core.systems.DarkTheme;

/**
 * Zero-Allocation Diagnostic Console.
 * 
 * <p>Real-time visual debugging system. Renders metrics and
 * visual feedback without generating pressure on the Garbage Collector.
 * 
 * @author Marvin Alexander Flores Canales
 * @since 1.0
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 16_666_000, minThroughput = 60, alignment = 0, lockFree = false, offHeap = false, notes = "Zero-GC Visual Debugger (Tick-Synchronized)")
public final class DarkNativeConsole {

    // Pre-allocated input buffer (Zero-Allocation)
    private final StringBuilder inputBuffer = new StringBuilder(64);
    
    // Text rendering buffer to avoid concatenations (Zero-Allocation)
    private final char[] renderBuffer = new char[80];

    // Rendering Resources (Immutable in the Data Segment)
    private static final Font CONSOLE_FONT = new Font("Monospaced", Font.BOLD, 18);
    private static final BasicStroke BASE_STROKE = new BasicStroke(2.0f);
    private static final Color BG_OVERLAY = new Color(0, 0, 0, 230);

    private float flashIntensity = 0.0f;
    private Color currentBorder = DarkTheme.MINT_NEON;

    /**
     * Character injection from InputBridge.
     * Manages the buffer without generating garbage on the Heap.
     * IMPLEMENTS HARDWARE INTERCEPT: Master Keys 1 and 0.
     */
    public void pushChar(char c) {
        // [HARDWARE INTERCEPT]: Master Key Protocol
        // These keys operate at interrupt level, bypassing the buffer.
        if (c == '1') {
            // System.out.println("[HARDWARE INTERCEPT] KEY '1' DETECTED -> STARTUP SEQUENCE INITIATED.");
            // In a real system, this would send signal 0x9001 (MarvinDevOn) to the Bus.
            // DarkAtomicBus.publish(DarkSignalCommands.ADMIN_CMD_ON, 0);
            return;
        }
        if (c == '0') {
            // System.out.println("[HARDWARE INTERCEPT] KEY '0' DETECTED -> SHUTDOWN SEQUENCE INITIATED.");
            // In a real system, this would send signal 0x9002 (MarvinDevoff) to the Bus.
            // DarkAtomicBus.publish(DarkSignalCommands.ADMIN_CMD_OFF, 0);
            return;
        }

        // [RESERVED]: x (Future Use)
        if (c == 'J' || c == 'j') {
            // System.out.println("x 'J' DETECTED -> No action assigned.");
            return;
        }

        // [HARDWARE]: Protocolo ('W')
        if (c == 'W' || c == 'w') {
            // System.out.println("'W' DETECTED -> ");
            // Legacy w bridge functionality removed for security
            return;
        }

        if (c == '\b' && inputBuffer.length() > 0) {
            inputBuffer.setLength(inputBuffer.length() - 1);
        } else if (c >= 32 && c <= 126 && inputBuffer.length() < 60) {
            inputBuffer.append(c);
        }
    }

    /**
     * Visual command feedback (Success/Failure).
     */
    public void triggerFeedback(boolean success) {
        currentBorder = success ? Color.GREEN : Color.RED;
        flashIntensity = 1.0f;
    }

    /**
     * Synchronous update with the Kernel.
     * Controls the decay of visual effects.
     */
    public void update(DarkStateVault vault) {
        if (flashIntensity > 0) {
            flashIntensity -= 0.05f;
            if (flashIntensity <= 0) {
                flashIntensity = 0;
                currentBorder = DarkTheme.MINT_NEON;
            }
        }
    }

    /**
     * Optimized rendering: Direct projection of the buffer to the GPU (via G2D).
     */
    public void render(Graphics2D g2d, DarkStateVault vault, int x, int y, int w, int h) {
        // Direct access to the engine's frequency (Time Control)
        int tick = vault.read(DarkStateLayout.SYS_TICK);

        // 1. Glassmorphism Background (Industrial Look)
        g2d.setColor(BG_OVERLAY);
        g2d.fillRoundRect(x, y, w, h, 8, 8);

        // 2. Reactive Border
        g2d.setStroke(BASE_STROKE);
        g2d.setColor(currentBorder);
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // 3. Text and Synchronized Cursor
        g2d.setFont(CONSOLE_FONT);
        g2d.setColor(Color.WHITE);

        // Synchronous blink: Zero external timers, purely Tick-based arithmetic.
        boolean showCursor = (tick % 60 < 30);

        // [MECHANICAL SYMPATHY]: Zero-Allocation rendering using pre-allocated buffers
        renderBuffer[0] = '>';
        renderBuffer[1] = ' ';
        
        int inputLen = inputBuffer.length();
        inputBuffer.getChars(0, inputLen, renderBuffer, 2);
        
        int totalLen = 2 + inputLen;
        if (showCursor) {
            renderBuffer[totalLen] = '_';
            totalLen++;
        }
        
        g2d.drawChars(renderBuffer, 0, totalLen, x + 15, y + 27);
    }
}
// updated 3/1/26
