// Reading Order: 00010100
package sv.volcan.core;

import java.awt.*;
import sv.volcan.state.VolcanStateVault;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.systems.VolcanTheme;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Consola de Diagnóstico Zero-Allocation.
 * DEPENDENCIAS: java.awt.Graphics2D, VolcanStateVault
 * MÉTRICAS: Zero-GC Render, Tick-Sync
 * 
 * Sistema visual de depuración en tiempo real. Renderiza métricas y
 * feedback visual sin generar presión sobre el Garbage Collector.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
@AAACertified(date = "2026-01-10", maxLatencyNs = 16_666_000, minThroughput = 60, alignment = 0, lockFree = false, offHeap = false, notes = "Zero-GC Visual Debugger (Tick-Synchronized)")
public final class VolcanNativeConsole {

    // Buffer de entrada pre-asignado (Zero-Allocation)
    private final StringBuilder inputBuffer = new StringBuilder(64);

    // Recursos de Renderizado (Inmutables en el Segmento de Datos)
    private static final Font CONSOLE_FONT = new Font("Monospaced", Font.BOLD, 18);
    private static final BasicStroke BASE_STROKE = new BasicStroke(2.0f);
    private static final Color BG_OVERLAY = new Color(0, 0, 0, 230);

    private float flashIntensity = 0.0f;
    private Color currentBorder = VolcanTheme.MINT_NEON;

    /**
     * Inyección de caracteres desde el InputBridge.
     * Gestiona el buffer sin generar basura en el Heap.
     */
    /**
     * Inyección de caracteres desde el InputBridge.
     * Gestiona el buffer sin generar basura en el Heap.
     * IMPLEMENTA HARDWARE INTERCEPT: Teclas Maestras 1 y 0.
     */
    public void pushChar(char c) {
        // [HARDWARE INTERCEPT]: Protocolo de Teclas Maestras
        // Estas teclas operan a nivel de interrupción, bypass del buffer.
        if (c == '1') {
            System.out.println("[HARDWARE INTERCEPT] KEY '1' DETECTED -> IGNITION SEQUENCE INITIATED.");
            // En un sistema real, esto enviaría la señal 0x9001 (MarvinDevOn) al Bus.
            // VolcanAtomicBus.publish(VolcanSignalCommands.MAGIC_CMD_ON, 0);
            return;
        }
        if (c == '0') {
            System.out.println("[HARDWARE INTERCEPT] KEY '0' DETECTED -> SHUTDOWN SEQUENCE INITIATED.");
            // En un sistema real, esto enviaría la señal 0x9002 (MarvinDevoff) al Bus.
            // VolcanAtomicBus.publish(VolcanSignalCommands.MAGIC_CMD_OFF, 0);
            return;
        }

        // [RESERVED]: x (Future Use)
        if (c == 'J' || c == 'j') {
            System.out.println("x 'J' DETECTED -> No action assigned.");
            return;
        }

        // [HARDWARE]: Protocolo ('W')
        if (c == 'W' || c == 'w') {
            System.out.println("'W' DETECTED -> ");
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
     * Feedback visual de comandos (Éxito/Fallo).
     */
    public void triggerFeedback(boolean success) {
        currentBorder = success ? Color.GREEN : Color.RED;
        flashIntensity = 1.0f;
    }

    /**
     * Actualización síncrona con el Kernel.
     * Controla el decaimiento de los efectos visuales.
     */
    public void update(VolcanStateVault vault) {
        if (flashIntensity > 0) {
            flashIntensity -= 0.05f;
            if (flashIntensity <= 0) {
                flashIntensity = 0;
                currentBorder = VolcanTheme.MINT_NEON;
            }
        }
    }

    /**
     * Renderizado optimizado: Proyección directa del buffer a la GPU (via G2D).
     */
    public void render(Graphics2D g2d, VolcanStateVault vault, int x, int y, int w, int h) {
        // Acceso directo a la frecuencia del motor (Soberanía de Tiempo)
        int tick = vault.read(VolcanStateLayout.SYS_TICK);

        // 1. Fondo Glassmorphism (Industrial Look)
        g2d.setColor(BG_OVERLAY);
        g2d.fillRoundRect(x, y, w, h, 8, 8);

        // 2. Borde Reactivo
        g2d.setStroke(BASE_STROKE);
        g2d.setColor(currentBorder);
        g2d.drawRoundRect(x, y, w, h, 8, 8);

        // 3. Texto y Cursor Sincronizado
        g2d.setFont(CONSOLE_FONT);
        g2d.setColor(Color.WHITE);

        // Parpadeo síncrono: Cero temporizadores externos, solo aritmética de Ticks.
        boolean showCursor = (tick % 60 < 30);

        // [NOTA TÉCNICA]: En fase final se sustituirá por un GlyphRenderer para
        // eliminar el '+' de concatenación.
        g2d.drawString("> " + inputBuffer.toString() + (showCursor ? "_" : ""), x + 15, y + 27);
    }
}
// actualizado3/1/26
