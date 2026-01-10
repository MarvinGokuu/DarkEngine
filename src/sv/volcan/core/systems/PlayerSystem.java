package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Marvin-Dev
 * RESPONSABILIDAD: Control de Avatar (Player Character).
 * DEPENDENCIAS: WorldStateFrame
 * MÉTRICAS: Zero-Jitter Input Response
 * 
 * Aplica comandos de entrada al estado del jugador.
 * Implementa movimiento determinista basado en el input del frame actual.
 * 
 * @author Marvin-Dev
 * @version 1.0
 * @since 2026-01-05
 */
public final class PlayerSystem implements SovereignSystem {

    // Offsets fijos en el WorldStateFrame (Soberanía de direccionamiento)
    private static final long ADDR_POS_X = 1000L;
    private static final long ADDR_POS_Y = 1008L;
    private static final long ADDR_INPUT = 1016L;

    // Constantes de movimiento (Inyectadas en el hot-path)
    private static final double BASE_VELOCITY = 300.0; // Píxeles por segundo

    /**
     * Procesa el movimiento del jugador sin instanciar objetos (Zero-Allocation).
     * 
     * IMPLEMENTACIÓN: SovereignSystem.update()
     * GARANTÍA: Determinista - mismo state + deltaTime = mismo resultado
     */
    @Override
    public void update(WorldStateFrame state, double deltaTime) {
        int direction = state.readInt(ADDR_INPUT);
        if (direction == 0)
            return;

        // Lectura de la fuente de verdad única (Memory Segment Off-Heap)
        double currentX = state.readDouble(ADDR_POS_X);
        double currentY = state.readDouble(ADDR_POS_Y);

        double moveStep = BASE_VELOCITY * deltaTime;

        // Jump Table optimizada por el JIT para despacho en pocos ciclos de CPU
        switch (direction) {
            case 1 -> currentY -= moveStep; // UP
            case 2 -> currentY += moveStep; // DOWN
            case 3 -> currentX -= moveStep; // LEFT
            case 4 -> currentX += moveStep; // RIGHT
        }

        // Escritura atómica de vuelta al silicio (Direct Memory Access)
        state.writeDouble(ADDR_POS_X, currentX);
        state.writeDouble(ADDR_POS_Y, currentY);
    }
    // actualizado3/1/26
}