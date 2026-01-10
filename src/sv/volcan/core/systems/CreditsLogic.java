package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Lógica de UI Determinista (Créditos).
 * DEPENDENCIAS: WorldStateFrame
 * MÉTRICAS: Fixed-Point Arithmetic
 * 
 * Sistema de lógica para elementos de UI. Calcula el desplazamiento de los
 * créditos
 * utilizando aritmética de punto fijo para mantener el determinismo.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class CreditsLogic {

    // Constantes de diseño (Offsets de memoria dentro del Frame)
    private static final long OFFSET_ACTIVE = 1024L;
    private static final long OFFSET_SCROLL = 1028L;

    /**
     * Actualiza la posición de los créditos.
     * [NOTA TÉCNICA]: El uso de deltaTime se permite aquí para el cálculo,
     * pero el resultado se persiste como INT (Punto Fijo) para garantizar
     * determinismo en replays.
     */
    public static void update(WorldStateFrame state, double deltaTime) {
        if (state.readInt(OFFSET_ACTIVE) == 0)
            return;

        int currentY = state.readInt(OFFSET_SCROLL);

        // Simpatía Mecánica: Escalamiento por factor 10,000 para preservar precisión en
        // enteros.
        // [OBSERVACIÓN]: Si se detecta cuello de botella en el casting, se recomienda
        // pre-calcular el scroll_step en el Heartbeat.
        currentY -= (int) (deltaTime * 10000);

        // Reset del ciclo de scroll (Valores normalizados para resolución interna)
        if (currentY < -30000)
            currentY = 72000;

        state.writeInt(OFFSET_SCROLL, currentY);
    }
    // actualizado3/1/26
}