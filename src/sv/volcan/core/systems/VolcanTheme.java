package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Definición de la estética industrial y estados visuales del
 * Runtime.
 * DISEÑO: Glassmorphism / Industrial High-Tech.
 * GARANTÍAS: Cero procesamiento de Strings, paleta pre-calculada en memoria
 * estática.
 * PROHIBICIONES: Prohibido el uso de archivos .CSS o configuraciones externas
 * pesadas.
 * DOMINIO CRÍTICO: Visualización y Telemetría.
 */
public final class VolcanTheme {

    // PALETA SOBERANA (Constantes de silicio - Accesibles en O(1))
    public static final Color MINT_NEON = new Color(0, 255, 163);
    public static final Color BACKGROUND = new Color(10, 10, 15);
    public static final Color PANEL_GLASS = new Color(30, 30, 45, 180);
    public static final Color ALERT_CRITICAL = new Color(220, 0, 40);
    public static final Color ALERT_HEALING = new Color(0, 180, 255);

    // [OPTIMIZACIÓN CACHÉ]: Stroke pre-instanciado para evitar asignación en el
    // loop de render
    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1f);

    private VolcanTheme() {
    }

    /**
     * Retorna el color de borde dinámico basado en el registro de alerta del Vault.
     * 
     * @param alertLevel Nivel extraído de VolcanStateLayout.SYS_ENGINE_FLAGS
     */
    public static Color getDynamicAccent(int alertLevel) {
        return switch (alertLevel) {
            case 1 -> ALERT_CRITICAL; // Estado Crítico
            case 2 -> ALERT_HEALING; // Auto-reparación
            default -> MINT_NEON; // Operación Normal
        };
    }

    /**
     * Definición del look de los paneles de telemetría.
     */
    public static void applyGlassStyle(Graphics2D g2d, int x, int y, int w, int h) {
        // Renderizado industrial de alta eficiencia
        g2d.setColor(PANEL_GLASS);
        g2d.fillRoundRect(x, y, w, h, 12, 12);

        // [OBSERVACIÓN]: Se recomienda usar DEFAULT_STROKE para evitar 'new' en cada
        // llamada.
        g2d.setStroke(DEFAULT_STROKE);
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawRoundRect(x, y, w, h, 12, 12);
    }
    // actualizado3/1/26
}
