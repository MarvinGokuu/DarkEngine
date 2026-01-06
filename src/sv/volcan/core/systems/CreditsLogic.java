package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: UI-Logic (Sub-sistema de Simulación)
 * RESPONSABILIDAD: Cálculo determinista del scroll de créditos mediante
 * aritmética de punto fijo.
 * GARANTÍAS: Zero-allocation, invariante de posición en el WorldState.
 * PROHIBICIONES: Prohibido almacenar tipos de punto flotante (float/double) en
 * el WorldStateFrame.
 * DOMINIO CRÍTICO: Tiempo y Estado.
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