package sv.volcan.net;

import java.awt.*;
import sv.volcan.state.VolcanStateVault;
import sv.volcan.state.VolcanStateLayout;
import sv.volcan.core.systems.VolcanTheme;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Visualización y monitoreo reactivo de signos vitales del
 * Kernel.
 * GARANTÍAS: Zero-Allocation en estado estable, sincronización total con el
 * Tick del motor.
 * PROHIBICIONES: Prohibido usar cálculos de tiempo real (System.ms) para
 * efectos visuales.
 * DOMINIO CRÍTICO: Telemetría / UX Industrial
 *
 * @author Marvin-Dev
 */
public final class VolcanTelemetryUnit {

    private static final Font UI_FONT = new Font("Courier New", Font.BOLD, 14);

    /**
     * Lógica de monitoreo: Analiza el pulso y activa protocolos de emergencia en el
     * Vault.
     */
    public void update(VolcanStateVault vault) {
        // Leemos métricas escaladas (0-10000)
        int loadVal = vault.read(VolcanStateLayout.METRIC_CPU_LOAD);

        // Umbral de Seguridad: 80% (8000 en escala industrial)
        boolean isCritical = loadVal > 8000;

        // Inyectamos bandera de estado: 1 = Estrés Crítico, 0 = Operación Nominal.
        // Esto permite que el WorkStealingProcessor o los Sistemas de Partículas cedan
        // ciclos.
        vault.write(VolcanStateLayout.SYS_ENGINE_FLAGS, isCritical ? 1 : 0);
    }

    /**
     * Renderizado de la consola de telemetría.
     * Utiliza el pulso determinista del motor para efectos de animación.
     */
    public void render(Graphics2D g2d, VolcanStateVault vault) {
        int loadVal = vault.read(VolcanStateLayout.METRIC_CPU_LOAD);
        int flags = vault.read(VolcanStateLayout.SYS_ENGINE_FLAGS);
        int tick = vault.read(VolcanStateLayout.SYS_TICK);

        // UI Style: Aplicación de la estética Glassmorphism definida en el Kernel UI
        VolcanTheme.applyGlassStyle(g2d, 20, 20, 250, 80);

        // [DETERMINISMO]: Parpadeo basado en el Tick actual (40 ticks por ciclo)
        boolean blink = (tick % 40 < 20);
        Color accent = VolcanTheme.getDynamicAccent(flags);

        g2d.setColor(flags == 1 && blink ? Color.WHITE : accent);
        g2d.setFont(UI_FONT);

        // Renderizado de métricas
        // Nota: loadVal / 100 convierte la base 10000 a porcentaje humano (0-100)
        drawMetric(g2d, "CPU_LOAD", loadVal / 100, 40, 45);

        // Barra de estado física (representación visual del buffer de carga)
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.fillRect(40, 60, 200, 8); // Fondo de la barra

        g2d.setColor(accent);
        g2d.fillRect(40, 60, (loadVal * 200) / 10000, 8); // Indicador de carga
    }

    /**
     * Dibuja la etiqueta técnica.
     * [OPTIMIZACIÓN FUTURA]: Implementar GlyphCache para eliminar la concatenación
     * de Strings.
     */
    private void drawMetric(Graphics2D g2d, String label, int value, int x, int y) {
        g2d.drawString(label + ": " + value + "%", x, y);
    }
}
// actualizado3/1/26
