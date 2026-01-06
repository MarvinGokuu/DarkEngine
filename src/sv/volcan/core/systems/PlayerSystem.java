package sv.volcan.core.systems; // Sincronizado con la ruta src/sv/volcan/core/systems/

import sv.volcan.state.WorldStateFrame;

/**
 * AUTORIDAD: Volcan
 * RESPONSABILIDAD: Mutación física del actor principal basada en el estado del
 * input.
 * GARANTÍAS: Movimiento atómico, cero-jitter mediante acceso directo a memoria
 * nativa.
 * PROHIBICIONES: Prohibido instanciar vectores (new Vector2D), prohibido el uso
 * de Strings para comandos de entrada.
 * DOMINIO CRÍTICO: Ejecución y Estado del Mundo.
 * 
 * PATRÓN: Strategy Pattern (implementa SovereignSystem)
 * PRINCIPIO SOLID: Open/Closed Principle
 * ROL: Software Engineer aplicando contrato de sistema
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