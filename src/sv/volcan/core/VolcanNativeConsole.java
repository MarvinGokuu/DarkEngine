package sv.volcan.core;

import java.awt.*;
import sv.volcan.state.VolcanStateVault;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.systems.VolcanTheme;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Consola de comando de baja latencia y Zero-Allocation para
 * telemetría.
 * GARANTÍAS: Renderizado por caché de recursos, sincronización por Tick-Rate
 * del motor.
 * PROHIBICIONES: Prohibido instanciar objetos Color o Font en el render;
 * prohibido el uso de Scanner.
 * DOMINIO CRÍTICO: Visualización e Interfaz de Control.
 */
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
    public void pushChar(char c) {
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
